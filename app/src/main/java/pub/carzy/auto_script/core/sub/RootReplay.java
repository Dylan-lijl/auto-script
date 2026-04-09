package pub.carzy.auto_script.core.sub;

import android.util.Log;
import android.view.KeyEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.lang.Pair;
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.core.data.ReplayModel;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * root模式
 *
 * @author admin
 */
public class RootReplay extends AbstractReplay<RootReplay.GesturePayload, RootReplay.KeyEventPayload> {
    private final AtomicInteger trackingId;
    private DataOutputStream gestureWriter;
    private DataOutputStream keyEventWriter;
    private final Pair<Process, EventDevice> gestureProcess;
    private final Pair<Process, EventDevice> keyEventProcess;
    private ThreadPoolExecutor executor;
    private final BlockingQueue<Payload> queue;

    public RootReplay(Pair<Process, EventDevice> gestureProcess, Pair<Process, EventDevice> keyEventProcess) {
        trackingId = new AtomicInteger((int) (System.currentTimeMillis() & 0xFFFF));
        this.gestureProcess = gestureProcess;
        this.keyEventProcess = keyEventProcess;
        queue = new LinkedBlockingQueue<>();
    }

    final Map<Integer, Integer> slotMap = new ConcurrentHashMap<>();

    /**
     * <a href="https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/native/services/inputflinger/tests/MultiTouchInputMapper_test.cpp;l=1?q=MultiTouchInputMapper_test&sq=">android多手势测试demo</a>
     */
    private void writeData() {
        int slot = 0;
        while (executor != null) {
            try {
                Payload poll = queue.poll(1, TimeUnit.SECONDS);
                if (poll == null || poll.isEmpty()) {
                    if (poll instanceof NullPayload) {
                        slotMap.clear();
                    } else if (poll instanceof BreakPayload) {
                        break;
                    }
                    poll = null;
                    continue;
                }
                int size = getEventStructSize();
                int oldActive = slotMap.size();
                List<Number[]> newData = new ArrayList<>(poll.getData().size() + 2);
                for (Number[] line : poll.getData()) {
                    if (line.length != 3) {
                        continue;
                    }
                    short type = line[0].shortValue();
                    short code = line[1].shortValue();
                    int value = line[2].intValue();
                    if (poll instanceof GesturePayload) {
                        if (type == InputConstants.EV_ABS && code == InputConstants.ABS_MT_SLOT) {
                            slot = value;
                        } else if (type == InputConstants.EV_ABS
                                && code == InputConstants.ABS_MT_TRACKING_ID) {
                            if (value == InputConstants.TRACKING_ID_END) {
                                slotMap.remove(slot);
                            } else {
                                slotMap.put(slot, value);
                            }
                        } else if (type == InputConstants.SYN_REPORT && code == InputConstants.SYN_REPORT) {
                            if (oldActive == 0 && !slotMap.isEmpty()) {
                                newData.add(new Number[]{InputConstants.EV_KEY, InputConstants.BTN_TOUCH, InputConstants.KEY_PRESS});
                            } else if (oldActive > 0 && slotMap.isEmpty()) {
                                newData.add(new Number[]{InputConstants.EV_KEY, InputConstants.BTN_TOUCH, InputConstants.KEY_RELEASE});
                            }
                        }
                    }
                    newData.add(line);
                }
                ByteBuffer buffer = ByteBuffer.allocate(size * newData.size());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                for (Number[] line : newData) {
                    // 16字节结构体：Time(8) + Type(2) + Code(2) + Value(4)
                    putData(buffer, size, line[0].shortValue(), line[1].shortValue(), line[2].intValue());
//                    Log.d("writeData", (size == 24 ? "0 " : "") + "0 " + line[0] + " " + line[1] + " " + line[2]);
                }
                poll.write(buffer.array());
                poll.flush();
                poll = null;
                newData = null;
            } catch (Exception e) {
                Log.e("RootReplay", "writeData:exception", e);
            }
        }
    }

    private static void putData(ByteBuffer buffer, int size, short type, short code, int value) {
        buffer.putLong(0);
        if (size == 24) {
            buffer.putLong(0);
        }
        buffer.putShort(type);
        buffer.putShort(code);
        buffer.putInt(value);
    }

    public int getEventStructSize() {
        // 获取系统架构
        String arch = System.getProperty("os.arch");
        // 绝大多数现代 64 位 Android (arm64-v8a) 使用 24 字节
        if (arch != null && (arch.contains("64") || arch.contains("aarch64"))) {
            return 24;
        }
        // 老旧的 32 位设备 (armeabi) 使用 16 字节
        return 16;
    }

    @Override
    protected void recover() {
        try {
            queue.put(new NullPayload());
        } catch (InterruptedException ignored) {
        }
        super.recover();
    }

    @Override
    protected void afterStartInit() {
        super.afterStartInit();
        this.gestureWriter = gestureProcess.getKey() == null ? null : new DataOutputStream(gestureProcess.getKey().getOutputStream());
        this.keyEventWriter = keyEventProcess.getKey() == null ? null : new DataOutputStream(keyEventProcess.getKey().getOutputStream());
        if (executor != null && !executor.isShutdown()) {
            try {
                queue.put(new BreakPayload());
            } catch (InterruptedException ignored) {
            }
            executor.shutdownNow();
        }
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        ThreadUtil.runOnCpu(() -> {
            if (gestureWriter != null) {
                try {
                    gestureWriter.writeBytes("cat > " + gestureProcess.getValue().getPath() + "\n");
                    gestureWriter.flush();
                } catch (Exception ignored) {
                }
            }
            if (keyEventWriter != null) {
                try {
                    keyEventWriter.writeBytes("cat > " + keyEventProcess.getValue().getPath() + "\n");
                    keyEventWriter.flush();
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void start() {
        super.start();
        if (status.get() == RUNNING && executor != null) {
            executor.submit(this::writeData);
        }
    }

    @Override
    protected KeyEventPayload createKeyEventPayload() {
        return new KeyEventPayload();
    }

    @Override
    protected boolean dispatchKeyEvent(KeyEventPayload payload) {
        if (payload.isEmpty() || keyEventWriter == null) {
            return false;
        }
        try {
            payload.binding(keyEventWriter);
            queue.put(payload);
        } catch (InterruptedException e) {
            Log.d("RootReplay", "dispatchKeyEvent: ", e);
        }
        return true;
    }

    @Override
    protected boolean dispatchGesture(GesturePayload payload) {
        if (payload.isEmpty() || gestureWriter == null) {
            return false;
        }
        try {
            payload.binding(gestureWriter);
            queue.put(payload);
        } catch (InterruptedException e) {
            Log.d("RootReplay", "dispatchGesture: ", e);
        }
        return true;
    }

    @Override
    public void pause() {
        try {
            queue.put(new NullPayload());
        } catch (InterruptedException ignored) {
        }
        super.pause();
    }

    @Override
    public void stop() {
        try {
            queue.put(new BreakPayload());
        } catch (InterruptedException ignored) {
        }
        super.stop();
        if (executor != null) {
            executor.shutdown();
            executor = null;
            if (gestureWriter != null) {
                try {
                    gestureWriter.writeBytes("\nexit\n");
                    gestureWriter.flush();
                } catch (Exception ignored) {
                }
            }
            if (keyEventWriter != null) {
                try {
                    keyEventWriter.writeBytes("\nexit\n");
                    keyEventWriter.flush();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    protected void releaseGesture(ReplayModel.ReplayActionModel value) {
        GesturePayload payload = createGesturePayload();
        payload.up(value.getIndex());
        payload.sync();
        dispatchGesture(payload);
        super.releaseGesture(value);
    }

    @Override
    protected void releaseKey(ReplayModel.ReplayActionModel value) {
        KeyEventPayload payload = createKeyEventPayload();
        payload.up(value.getCode());
        payload.sync();
        dispatchKeyEvent(payload);
        super.releaseKey(value);
    }

    @Override
    protected void processGestureAction(GesturePayload payload, ReplayModel.ReplayActionModel root, ReplayModel.ReplayActionModel action, AtomicBoolean unfinished) {
        if (action == null || action.getPoints().isEmpty()) {
            return;
        }
        //查看索引位
        int max = action.getPoints().size() - 1;
        //已处理
        if (action.getCurrent().get() > max) {
            return;
        }
        if (action.getTrackingId() == null) {
            action.setTrackingId(trackingId.getAndUpdate(v -> (v + 1) & 0x7FFFFFFF));
        }
        long t = tick.get();
        boolean synced = false;
        //遍历点
        while (t > 0 && action.getCurrent().get() <= max) {
            int i = action.getCurrent().get();
            ReplayModel.ReplayPointModel point = action.getPoints().get(i);
            //这里其实还是有点问题,如果间隔时间大于时间片,可能不准确,应该需要记录剩余时间
            long reTime = point.getRemainingTime().get();
            if (reTime <= 0) {
                //一般情况下不会出现这个问题
                action.getCurrent().incrementAndGet();
                continue;
            }
            long cost = Math.min(t, reTime);
            if (!point.isDispatched()) {
                //第一个点代表按下
                if (i == 0) {
                    payload.down(action.getIndex(), action.getTrackingId(), point.getX(), point.getY());
                    point.setDispatched(true);
                } else {
                    // move 只在首次进入触发
                    payload.move(action.getIndex(), point.getX(), point.getY());
                    point.setDispatched(true);
                }
            }
            // 时间推进
            t -= cost;
            point.getRemainingTime().set(reTime - cost);
            // 当前点完成
            if (point.getRemainingTime().get() <= 0) {
                action.getCurrent().incrementAndGet();
            }
            // 重点：如果到达最后一个点，必须先发送当前坐标的 sync，再发送抬起信号
            // 最后一点
            if (i == max && point.getRemainingTime().get() <= 0) {
                if (!point.isDispatched()) {
                    payload.move(action.getIndex(), point.getX(), point.getY());
                    point.setDispatched(true);
                }
                if (!payload.isEmpty()) {
                    payload.sync();
                }
                payload.up(action.getIndex());
                payload.sync();
                synced = true;
                break;
            }
        }
        if (!synced && !payload.isEmpty()) {
            payload.sync();
        }
        ReplayModel.ReplayPointModel lastPoint = action.getPoints().get(max);
        // 只有在确定没做完时才更新，不要覆盖之前的 true
        if (!(lastPoint.isDispatched() && lastPoint.getRemainingTime().get() <= 0)) {
            unfinished.set(true);
        }
    }

    @Override
    protected GesturePayload createGesturePayload() {
        return new GesturePayload();
    }

    @Override
    protected void processCodeAction(KeyEventPayload payload, ReplayModel.ReplayActionModel root, ReplayModel.ReplayActionModel model, AtomicBoolean unfinished) {
        if (model == null || model.getCode() == null || model.getRemainingTime().get() <= 0) {
            return;
        }
        //如果剩余时间等于时长说明未开始,则需要准备按下事件
        if (model.getRemainingTime().get() == model.getDuration()) {
            payload.down(model.getCode());
        }
        //扣除剩余时间
        model.getRemainingTime().set(model.getRemainingTime().get() - tick.get());
        //剩余时间等于小于0说明时间片用完了,可以发送抬起事件
        if (model.getRemainingTime().get() <= 0) {
            payload.up(model.getCode());
        } else {
            //还有剩余时间就标记成未完成,防止被移除
            unfinished.set(true);
        }
        if (!payload.isEmpty()) {
            payload.sync();
        }
    }

    @Override
    public void close() {
        super.close();
        if (gestureProcess.getKey() != null) {
            if (gestureWriter != null) {
                try {
                    gestureWriter.close();
                } catch (IOException ignored) {
                }
                gestureProcess.getKey().destroy();
            }
        }
        if (keyEventProcess.getKey() != null) {
            if (keyEventWriter != null) {
                try {
                    keyEventWriter.close();
                } catch (IOException ignored) {
                }
                keyEventProcess.getKey().destroy();
            }
        }
    }

    public static class GesturePayload extends AbstractPayload {
        private final List<Number[]> cmd;

        public GesturePayload() {
            cmd = new ArrayList<>();
        }

        public void down(int index, int trackingId, Float x, Float y) {
            //手指索引位
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_SLOT, index});
            //身份id
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_TRACKING_ID, trackingId});
            //按下
//            cmd.add(new Number[]{InputConstants.EV_KEY, InputConstants.BTN_TOUCH, InputConstants.KEY_PRESS});
            //x
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_POSITION_X, x.intValue()});
            //y
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_POSITION_Y, y.intValue()});
        }

        public void sync() {
            cmd.add(new Number[]{InputConstants.EV_SYN, InputConstants.SYN_REPORT, InputConstants.EMPTY});
        }

        public void up(int index) {
            //手势索引
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_SLOT, index});
//            cmd.add(new Number[]{InputConstants.EV_KEY, InputConstants.BTN_TOUCH, InputConstants.KEY_RELEASE});
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_TRACKING_ID, InputConstants.TRACKING_ID_END});
        }

        public void move(int index, Float x, Float y) {
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_SLOT, index});
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_POSITION_X, x.intValue()});
            cmd.add(new Number[]{InputConstants.EV_ABS, InputConstants.ABS_MT_POSITION_Y, y.intValue()});
        }

        @Override
        public boolean isEmpty() {
            return cmd.isEmpty();
        }

        @Override
        public int size() {
            return cmd.size();
        }

        @Override
        public Collection<Number[]> getData() {
            return cmd;
        }
    }

    public static class KeyEventPayload extends AbstractPayload {
        private final List<Number[]> events;

        public KeyEventPayload() {
            events = new ArrayList<>();
        }

        public void down(Integer code) {
            events.add(new Number[]{InputConstants.EV_KEY, code, KeyEvent.ACTION_DOWN});
        }

        public void up(Integer code) {
            events.add(new Number[]{InputConstants.EV_KEY, code, KeyEvent.ACTION_UP});
        }

        public void sync() {
            events.add(new Number[]{InputConstants.EV_SYN, InputConstants.SYN_REPORT, InputConstants.EMPTY});
        }

        @Override
        public boolean isEmpty() {
            return events.isEmpty();
        }

        @Override
        public int size() {
            return events.size();
        }

        @Override
        public Collection<Number[]> getData() {
            return events;
        }
    }
}
