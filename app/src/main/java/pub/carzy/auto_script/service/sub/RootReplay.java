package pub.carzy.auto_script.service.sub;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.KeyEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.lang.Pair;
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.utils.EventDeviceUtil;
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
        trackingId = new AtomicInteger(1);
        this.gestureProcess = gestureProcess;
        this.keyEventProcess = keyEventProcess;
        queue = new LinkedBlockingQueue<>();
    }

    private void writeData() {
        while (executor != null) {
            try {
                Payload poll = queue.poll(1, TimeUnit.SECONDS);
                if (poll == null || poll.isEmpty() ||
                        !(poll instanceof GesturePayload || poll instanceof KeyEventPayload)) {
                    continue;
                }
                int size = getEventStructSize();
                ByteBuffer buffer = ByteBuffer.allocate(size * poll.size());
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                List<Number[]> data = poll instanceof GesturePayload ? ((GesturePayload) poll).cmd : ((KeyEventPayload) poll).events;
                for (Number[] line : data) {
                    if (line.length != 3) {
                        continue;
                    }
                    short type = line[0].shortValue();
                    short code = line[1].shortValue();
                    int value = line[2].intValue();
                    // 16字节结构体：Time(8) + Type(2) + Code(2) + Value(4)
                    buffer.putLong(0);
                    if (size == 24) {
                        buffer.putLong(0);
                    }
                    buffer.putShort(type);
                    buffer.putShort(code);
                    buffer.putInt(value);
                }
                // 一次性批量写入管道，效率最高
                if (poll instanceof GesturePayload) {
                    gestureWriter.write(buffer.array());
                    gestureWriter.flush();
                } else {
                    keyEventWriter.write(buffer.array());
                    keyEventWriter.flush();
                }
            } catch (Exception e) {
                Log.e("RootReplay", "writeData:exception", e);
            }
        }
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

    private boolean hasCmd() {
        return !queue.isEmpty();
    }

    @Override
    protected void doSelfInit() {
        super.doSelfInit();
        this.gestureWriter = gestureProcess.getKey() == null ? null : new DataOutputStream(gestureProcess.getKey().getOutputStream());
        this.keyEventWriter = keyEventProcess.getKey() == null ? null : new DataOutputStream(keyEventProcess.getKey().getOutputStream());
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
            queue.put(payload);
        } catch (InterruptedException e) {
            Log.d("RootReplay", "dispatchKeyEvent: ", e);
        }
        return true;
    }

    @Override
    protected boolean dispatchGesture(GesturePayload payload) {
        if (payload.empty || gestureWriter == null) {
            return false;
        }
        try {
            queue.put(payload);
        } catch (InterruptedException e) {
            Log.d("RootReplay", "dispatchGesture: ", e);
        }
        return true;
    }

    @Override
    public void stop() {
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
    protected void processGestureAction(GesturePayload payload, ReplayModel.ReplayActionModel action, AtomicBoolean unfinished) {
        if (action == null || action.getPoints().isEmpty()) {
            return;
        }
        //查看索引位
        int current = action.getCurrent().get();
        int max = action.getPoints().size() - 1;
        //已处理
        if (current > max) {
            return;
        }
        if (action.getTrackingId() == null) {
            action.setTrackingId(trackingId.getAndAdd(1));
        }
        long t = tick.get();
        //遍历点
        for (int i = current; i < action.getPoints().size(); i++) {
            ReplayModel.ReplayPointModel point = action.getPoints().get(i);
            //这里其实还是有点问题,如果间隔时间大于时间片,可能不准确,应该需要记录剩余时间
            t = t - point.getDeltaTime();
            if (i == 0) {
                payload.down(action.getIndex(), action.getTrackingId(), point.getX(), point.getY());
            } else {
                payload.move(action.getIndex(), point.getX(), point.getY());
            }
            action.getCurrent().set(i + 1);
            // 重点：如果到达最后一个点，必须先发送当前坐标的 sync，再发送抬起信号
            if (i == max) {
                if (!payload.isEmpty()) {
                    payload.sync();
                }
                payload.up(action.getIndex());
                payload.sync();
                break;
            }

            // 时间片用完，仅针对 Move 过程
            if (t <= 0) {
                payload.sync();
                break;
            }
        }
        payload.empty = payload.cmd.isEmpty();
        if (action.getCurrent().get() <= max) {
            unfinished.set(true);
        }
    }

    @Override
    protected GesturePayload createGesturePayload() {
        return new GesturePayload();
    }

    @Override
    protected void processCodeAction(KeyEventPayload payload, ReplayModel.ReplayActionModel model, AtomicBoolean unfinished) {
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

    public static class GesturePayload implements Payload {
        private boolean empty;
        private final List<Number[]> cmd;

        public GesturePayload() {
            empty = true;
            cmd = new ArrayList<>();
        }

        public void down(int index, int trackingId, Float x, Float y) {
            //手指索引位
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_SLOT, index});
            //身份id
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_TRACKING_ID, trackingId});
            //按下
            cmd.add(new Number[]{EventDeviceUtil.EV_KEY, EventDeviceUtil.BTN_TOUCH, EventDeviceUtil.KEY_PRESS});
            //x
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_X, x.intValue()});
            //y
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_Y, y.intValue()});
        }

        public void sync() {
            cmd.add(new Number[]{EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN});
        }

        @SuppressLint("DefaultLocale")
        private String format(int type, int c, int v) {
            return String.format("%d %d %d", type, c, v);
        }

        public void up(int index) {
            //手势索引
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_SLOT, index});
            cmd.add(new Number[]{EventDeviceUtil.EV_KEY, EventDeviceUtil.BTN_TOUCH, EventDeviceUtil.KEY_RELEASE});
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_TRACKING_ID, -1});
        }

        public void move(int index, Float x, Float y) {
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_SLOT, index});
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_X, x.intValue()});
            cmd.add(new Number[]{EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_Y, y.intValue()});
        }

        @Override
        public boolean isEmpty() {
            return cmd.isEmpty();
        }

        @Override
        public int size() {
            return cmd.size();
        }
    }

    public static class KeyEventPayload implements Payload {
        private final List<Number[]> events;

        public KeyEventPayload() {
            events = new ArrayList<>();
        }

        public void down(Integer code) {
            events.add(new Number[]{EventDeviceUtil.EV_KEY, code, KeyEvent.ACTION_DOWN});
        }

        public void up(Integer code) {
            events.add(new Number[]{EventDeviceUtil.EV_KEY, code, KeyEvent.ACTION_UP});
        }

        @SuppressLint("DefaultLocale")
        private String format(int type, int c, int v) {
            return String.format("%d %d %d", type, c, v);
        }

        public void sync() {
            events.add(new Number[]{EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN});
        }

        @Override
        public boolean isEmpty() {
            return events.isEmpty();
        }

        @Override
        public int size() {
            return events.size();
        }
    }
}
