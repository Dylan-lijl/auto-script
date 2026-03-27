package pub.carzy.auto_script.core.sub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.Stopwatch;

/**
 * @author admin
 */
public class GestureRecorder extends AbstractGetEventRecorder<MotionEntity> {
    // 状态追踪变量
    private final Map<Integer, MotionEntity> motionMap;
    private final Map<Integer, PointEntity> activeStateMap;
    private final Set<Integer> dirtySlots;
    private final AtomicInteger slot;

    public GestureRecorder(Stopwatch stopwatch) {
        super(stopwatch);
        motionMap = new HashMap<>();
        activeStateMap = new HashMap<>();
        dirtySlots = new HashSet<>();
        slot = new AtomicInteger(0);
    }

    @Override
    public void clear() {
        motionMap.clear();
        activeStateMap.clear();
        dirtySlots.clear();
    }

    @Override
    protected void handleEvent(int type, int code, int value, long elapsed, OnRecordListener<MotionEntity> listener) {
        // 1. 处理 Slot 切换 (EV_ABS: 0003, ABS_MT_SLOT: 002f)
        if (type == InputConstants.EV_ABS && code == InputConstants.ABS_MT_SLOT) {
            slot.set(value);
            return;
        }
        int currentSlot = slot.get();
        // 2. 处理 Tracking ID (按下/抬起) (EV_ABS: 0003, ABS_MT_TRACKING_ID: 0039)
        if (type == InputConstants.EV_ABS && code == InputConstants.ABS_MT_TRACKING_ID) {
            if (value != -1) {
                // 手指按下
                MotionEntity motion = new MotionEntity();
                motion.setIndex(currentSlot);
                motion.setDownTime(elapsed);
                motionMap.put(currentSlot, motion);
                activeStateMap.put(currentSlot, new PointEntity());
            } else {
                // 手指抬起：先提交最后一帧，再移除
                commitPoint(currentSlot, elapsed);
                MotionEntity motion = motionMap.remove(currentSlot);
                if (motion != null && listener != null) {
                    listener.onCaptured(motion);
                }
                activeStateMap.remove(currentSlot);
                dirtySlots.remove(currentSlot);
            }
            return;
        }

        // 3. 坐标轴更新 (X: 0035, Y: 0036)
        if (type == InputConstants.EV_ABS) {
            PointEntity currentPoint = activeStateMap.get(currentSlot);
            if (currentPoint != null) {
                if (code == InputConstants.ABS_MT_POSITION_X) {
                    currentPoint.setX((float) value);
                    dirtySlots.add(currentSlot);
                } else if (code == InputConstants.ABS_MT_POSITION_Y) {
                    currentPoint.setY((float) value);
                    dirtySlots.add(currentSlot);
                }
            }
        }

        // 4. 同步帧结束 (EV_SYN: 0000, SYN_REPORT: 0000)
        if (type == InputConstants.EV_SYN && code == InputConstants.SYN_REPORT) {
            for (Integer slot : dirtySlots) {
                commitPoint(slot, elapsed);
            }
            dirtySlots.clear();
        }
    }

    private void commitPoint(int slot, long elapsed) {
        MotionEntity motion = motionMap.get(slot);
        PointEntity state = activeStateMap.get(slot);

        // 只有当 X 和 Y 都至少被初始化过一次时才保存
        if (motion != null && state != null && state.getX() != null && state.getY() != null) {
            PointEntity snapshot = new PointEntity();
            snapshot.setX(state.getX());
            snapshot.setY(state.getY());
            snapshot.setTime(elapsed - motion.getDownTime());
            motion.getPoints().add(snapshot);
        }
    }

}