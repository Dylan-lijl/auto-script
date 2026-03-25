package pub.carzy.auto_script.service.sub;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.carzy.auto_script.service.data.ReplayModel;

/**
 * 无障碍回放
 *
 * @author admin
 */
public class AccessibilityReplay extends AbstractReplay<AccessibilityReplay.GesturePayload, AccessibilityReplay.KeyEventPayload> {
    /**
     * 无障碍
     */
    private final AccessibilityService service;

    public AccessibilityReplay(AccessibilityService service) {
        super();
        this.service = service;
    }

    private final AccessibilityService.GestureResultCallback gestureCallback = new AccessibilityService.GestureResultCallback() {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            super.onCompleted(gestureDescription);
            Log.d(AccessibilityReplay.this.getClass().getCanonicalName(), "GestureResultCallback#onCompleted: ");
        }

        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            super.onCancelled(gestureDescription);
            Log.d(AccessibilityReplay.this.getClass().getCanonicalName(), "GestureResultCallback#onCancelled: ");
        }
    };

    @Override
    protected GesturePayload createGesturePayload() {
        return new GesturePayload();
    }

    /**
     * 处理手势action
     *
     * @param payload    负载
     * @param action     action
     * @param unfinished 未完成
     */
    @Override
    protected void processGestureAction(GesturePayload payload, ReplayModel.ReplayActionModel action, AtomicBoolean unfinished) {
        if (action == null) {
            return;
        }
        //查看索引位
        int current = action.getCurrent().get();
        int max = action.getPoints().size() - 1;
        //已处理
        if (current >= max) {
            return;
        }
        //构建path
        Path path = new Path();
        //遍历点
        for (int i = current; i < action.getPoints().size(); i++) {
            ReplayModel.ReplayPointModel point = action.getPoints().get(i);
            //第一次调用moveTo
            if (i == current) {
                path.moveTo(point.getX(), point.getY());
                continue;
            }
            //后续通通调用lineTo
            path.lineTo(point.getX(), point.getY());
        }
        //由于无障碍模拟手势无法将按下,移动,抬起分开,所以直接构建全部,也就是说忽略时间片限制,同时无障碍手势只允许15秒内,所以大于15秒可能会执行异常
        payload.builder.addStroke(new GestureDescription.StrokeDescription(path, 0, Math.max(1, action.getDuration())));
        //将current设置为size来标记当前已被处理
        action.getCurrent().set(max);
        payload.empty = false;
    }

    @Override
    protected void processCodeAction(KeyEventPayload payload, ReplayModel.ReplayActionModel model, AtomicBoolean unfinished) {
        if (model == null || model.getCode() == null || model.getRemainingTime().get() <= 0) {
            return;
        }
        //如果剩余时间等于时长说明未开始,则需要准备按下事件
        if (model.getRemainingTime().get() == model.getDuration()) {
            payload.events.add(new KeyEvent(KeyEvent.ACTION_DOWN, model.getCode()));
            payload.empty = false;
        }
        //扣除剩余时间
        model.getRemainingTime().set(model.getRemainingTime().get() - tick.get());
        //剩余时间等于小于0说明时间片用完了,可以发送抬起事件
        if (model.getRemainingTime().get() <= 0) {
            payload.events.add(new KeyEvent(KeyEvent.ACTION_UP, model.getCode()));
            payload.empty = false;
        } else {
            //还有剩余时间就标记成未完成,防止被移除
            unfinished.set(true);
        }
    }

    @Override
    protected KeyEventPayload createKeyEventPayload() {
        return new KeyEventPayload();
    }

    @Override
    protected boolean dispatchGesture(GesturePayload payload) {
        if (payload.empty) {
            return false;
        }
        return service.dispatchGesture(payload.builder.build(), gestureCallback, null);
    }

    @Override
    protected boolean dispatchKeyEvent(KeyEventPayload payload) {
        if (payload.empty) {
            return false;
        }
        for (KeyEvent event : payload.events) {
            service.performGlobalAction(event.getKeyCode());
        }
        return true;
    }

    public static class GesturePayload implements Payload {
        private boolean empty;
        private final GestureDescription.Builder builder;

        public GesturePayload() {
            empty = true;
            builder = new GestureDescription.Builder();
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public int size() {
            return isEmpty() ? 0 : 1;
        }
    }

    public static class KeyEventPayload implements Payload {
        private boolean empty;
        private final List<KeyEvent> events;

        public KeyEventPayload() {
            empty = true;
            events = new ArrayList<>();
        }

        @Override
        public boolean isEmpty() {
            return empty;
        }

        @Override
        public int size() {
            return events.size();
        }
    }
}
