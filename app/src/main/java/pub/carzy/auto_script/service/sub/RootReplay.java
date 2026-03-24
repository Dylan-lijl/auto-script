package pub.carzy.auto_script.service.sub;

import android.annotation.SuppressLint;
import android.view.KeyEvent;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.utils.EventDeviceUtil;

/**
 * root模式
 *
 * @author admin
 */
public class RootReplay extends SimpleReplay<RootReplay.GesturePayload, RootReplay.KeyEventPayload> {
    private final AtomicInteger trackingId;
    private final PrintWriter writer;

    private final String gestureEventPath;
    private final String keyEventPath;

    public RootReplay(Process process, String gestureEventPath, String keyEventPath) {
        writer = new PrintWriter(process.getOutputStream());
        trackingId = new AtomicInteger(1);
        this.gestureEventPath = gestureEventPath;
        this.keyEventPath = keyEventPath;
    }

    @Override
    protected KeyEventPayload createKeyEventPayload() {
        return new KeyEventPayload();
    }

    @Override
    protected boolean performGlobalAction(KeyEventPayload payload) {
        if (payload.empty || keyEventPath == null) {
            return false;
        }
        for (String line : payload.events) {
            writer.println(String.format("sendevent %s %s", keyEventPath, line));
        }
        writer.flush();
        return true;
    }

    @Override
    protected boolean dispatchGesture(GesturePayload payload) {
        if (payload.empty || gestureEventPath == null) {
            return false;
        }
        for (String line : payload.cmd) {
            writer.println(String.format("sendevent %s %s", gestureEventPath, line));
        }
        writer.flush();
        return true;
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
        if (current >= max) {
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
                //第一次是按下
                payload.down(action.getIndex(), action.getTrackingId(), point.getX(), point.getY());
            } else {
                payload.move(action.getIndex(), point.getX(), point.getY());
            }
            if (i == max) {
                payload.up(action.getIndex());
            }
            //时间片用完
            if (t <= 0 || i == max) {
                payload.sync();
                action.getCurrent().set(i + 1);
                break;
            }
        }
        payload.empty = payload.cmd.isEmpty();
        if (action.getCurrent().get() < max) {
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
            payload.empty = false;
        }
        //扣除剩余时间
        model.getRemainingTime().set(model.getRemainingTime().get() - tick.get());
        //剩余时间等于小于0说明时间片用完了,可以发送抬起事件
        if (model.getRemainingTime().get() <= 0) {
            payload.up(model.getCode());
            payload.empty = false;
        } else {
            //还有剩余时间就标记成未完成,防止被移除
            unfinished.set(true);
        }
        if (!payload.empty) {
            payload.sync();
        }
    }

    public static class GesturePayload implements Payload {
        private boolean empty;
        private final List<String> cmd;

        public GesturePayload() {
            empty = true;
            cmd = new ArrayList<>();
        }

        public void down(int index, int trackingId, Float x, Float y) {
            //手指索引位
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_SLOT, index));
            //身份id
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_TRACKING_ID, trackingId));
            //x
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_X, x.intValue()));
            //y
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_Y, y.intValue()));
        }

        public void sync() {
            cmd.add(format(EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN));
        }

        @SuppressLint("DefaultLocale")
        private String format(int type, int c, int v) {
            return String.format("%d %d %d", type, c, v);
        }

        public void up(int index) {
            //手势索引
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_SLOT, index));
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_TRACKING_ID, -1));
        }

        public void move(int index, Float x, Float y) {
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_SLOT, index));
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_X, x.intValue()));
            cmd.add(format(EventDeviceUtil.EV_ABS, EventDeviceUtil.ABS_MT_POSITION_Y, y.intValue()));
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }
    }

    public static class KeyEventPayload implements Payload {
        private boolean empty;
        private final List<String> events;

        public KeyEventPayload() {
            empty = true;
            events = new ArrayList<>();
        }

        public void down(Integer code) {
            events.add(format(EventDeviceUtil.EV_KEY, code, KeyEvent.ACTION_DOWN));
        }

        public void up(Integer code) {
            events.add(format(EventDeviceUtil.EV_KEY, code, KeyEvent.ACTION_UP));
        }

        @SuppressLint("DefaultLocale")
        private String format(int type, int c, int v) {
            return String.format("%d %d %d", type, c, v);
        }

        public void sync() {
            events.add(format(EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN, EventDeviceUtil.EV_SYN));
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }
    }
}
