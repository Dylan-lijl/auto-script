package pub.carzy.auto_script.service.sub;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class GestureRecorder implements RecorderLifeCycle<MotionEntity>{
    private static final String TAG = "GestureRecorder";
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;
    // 状态追踪变量
    private final Map<Integer, MotionEntity> motionMap;
    private final Map<Integer, PointEntity> activeStateMap;
    private final Set<Integer> dirtySlots;
    private Runnable runnable;
    private final Stopwatch stopwatch;

    public GestureRecorder(Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
        isRunning = new AtomicBoolean(false);
        isPaused = new AtomicBoolean(false);
        motionMap = new HashMap<>();
        activeStateMap = new HashMap<>();
        dirtySlots = new HashSet<>();
    }

    @Override
    public void start(String devicePath, OnRecordListener<MotionEntity> listener) {
        if (isRunning.get()){
            return;
        }
        isRunning.set(true);
        motionMap.clear();
        activeStateMap.clear();
        dirtySlots.clear();
        ThreadUtil.runOnCpu(() -> {
            Process process = null;
            try {
                process = Shell.getRootProcess();
                OutputStream os = process.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // 启动 getevent
                os.write(("getevent -t " + devicePath + "& echo "+START_CONTENT+"\n").getBytes());
                os.flush();
                runnable = () -> {
                    try {
                        Thread.sleep(5);
                        os.write(("echo "+END_CONTENT+"\n").getBytes());
                        os.flush();
                    } catch (Exception e) {
                        Log.d(TAG, "写ctrl c命令失败!", e);
                    }
                };
                AtomicInteger currentSlot = new AtomicInteger(0);
                String line;
                boolean started = false;
                while (isRunning.get() && (line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.contains(START_CONTENT)) {
                        started = true;
                        continue;
                    }
                    if (line.contains(END_CONTENT)) {
                        break;
                    }
                    //过滤不是数据行,暂停状态以及未解析到开始符
                    if (!line.contains("]") || isPaused.get() || !started) continue;

                    // 解析标准格式: [ timestamp] type code value
                    String[] parts = line.substring(line.indexOf("]") + 1).trim().split("\\s+");
                    if (parts.length < 3) continue;

                    int type = Integer.parseInt(parts[0], 16);
                    int code = Integer.parseInt(parts[1], 16);
                    // 使用Long解析防止十六进制符号位溢出
                    int value = (int) Long.parseLong(parts[2], 16);

                    handleEvent(currentSlot, type, code, value, stopwatch.getElapsedMillis(), listener);
                }
                Log.d(TAG, "正常结束......");
            } catch (Exception e) {
                Log.e(TAG, "Read Error: " + e.getMessage());
            } finally {
                if (process != null) process.destroy();
            }
        });
    }

    private void handleEvent(AtomicInteger c, int type, int code, int value, long elapsed, OnRecordListener<MotionEntity> listener) {
        // 1. 处理 Slot 切换 (EV_ABS: 0003, ABS_MT_SLOT: 002f)
        if (type == InputConstants.EV_ABS && code == InputConstants.ABS_MT_SLOT) {
            c.set(value);
            return;
        }
        int currentSlot = c.get();
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
                if (motion != null) {
                    if (listener != null) listener.onCaptured(motion);
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
    @Override
    public void stop() {
        isRunning.set(false);
        if (runnable != null) {
            ThreadUtil.runOnCpu(runnable);
        }
    }
    @Override
    public void pause() {
        isPaused.set(true);
    }

    /**
     * 恢复录制
     */
    @Override
    public void resume() {
        isPaused.set(false);
    }

    @Override
    public void destroy() {

    }

}