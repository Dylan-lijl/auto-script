package pub.carzy.auto_script.core.sub;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.carzy.auto_script.core.data.ReplayModel;

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

    @Override
    protected GesturePayload createGesturePayload() {
        return new GesturePayload();
    }

    @Override
    protected void doSelfInit() {
        //这里需要重构map,将冲突的手势要放在一起
        ReplayModel model = getModel();
        ConcurrentSkipListMap<Long, ReplayModel.ReplayActionModel> newMap = new ConcurrentSkipListMap<>();
        ConcurrentSkipListMap<Long, ReplayModel.ReplayActionModel> oldMap = model.getActionWaitMap();
        List<Long> keys = new ArrayList<>(oldMap.keySet());

        for (int i = 0; i < keys.size(); i++) {
            Long key = keys.get(i);
            ReplayModel.ReplayActionModel value = model.getActionWaitMap().get(key);
            if (value == null || value.isMerged()) {
                continue;
            }
            value.setMerged(true);
            long max = value.getDuration();
            for (ReplayModel.ReplayActionModel v : value.getParallel()) {
                v.setMerged(true);
                max = Math.max(max, v.getDuration());
            }
            //
            long nextKey = key + max;
            int last = i;
            for (int j = i + 1; j < keys.size(); j++) {
                Long next = keys.get(j);
                if (nextKey >= next) {
                    //合并并扁平化到一层集合当中
                    ReplayModel.ReplayActionModel t = oldMap.get(next);
                    if (t != null) {
                        t.setMerged(true);
                        nextKey = Math.max(next + t.getDuration(), nextKey);
                        value.getParallel().add(t);
                        for (ReplayModel.ReplayActionModel line : t.getParallel()) {
                            nextKey = Math.max(next + line.getDuration(), nextKey);
                            line.setMerged(true);
                            value.getParallel().add(line);
                        }
                        t.getParallel().clear();
                    }
                    //更新索引
                    last = j;
                } else {
                    break;
                }
            }
            i = last;
            //修正间隔时长
            value.setDuration(nextKey - key);
            newMap.put(key, value);
        }
        model.setActionWaitMap(newMap);
        super.doSelfInit();
    }

    /**
     * 处理手势action
     *
     * @param payload    负载
     * @param current    action
     * @param unfinished 未完成
     */
    @Override
    protected void processGestureAction(GesturePayload payload, ReplayModel.ReplayActionModel root, ReplayModel.ReplayActionModel current, AtomicBoolean unfinished) {
        if (current == null) {
            return;
        }
        //查看索引位
        int c = current.getCurrent().get();
        int max = current.getPoints().size() - 1;
        //已处理
        if (c >= max) {
            return;
        }
        //构建path
        Path path = new Path();
        //遍历点
        for (int i = c; i < current.getPoints().size(); i++) {
            ReplayModel.ReplayPointModel point = current.getPoints().get(i);
            //第一次调用moveTo
            if (i == c) {
                path.moveTo(point.getX(), point.getY());
                continue;
            }
            //后续通通调用lineTo
            path.lineTo(point.getX(), point.getY());
            //统统设置为0
            point.getRemainingTime().set(0);
        }
        //由于无障碍模拟手势无法将按下,移动,抬起分开,所以直接构建全部,也就是说忽略时间片限制,同时无障碍手势只允许15秒内,所以大于15秒可能会执行异常
        payload.builder.addStroke(new GestureDescription.StrokeDescription(path, current.getStartTime() - root.getStartTime(), Math.max(1, current.getDuration())));
        //将current设置为size来标记当前已被处理
        current.getCurrent().set(max);
        payload.empty = false;
    }

    @Override
    protected void processCodeAction(KeyEventPayload payload, ReplayModel.ReplayActionModel root, ReplayModel.ReplayActionModel model, AtomicBoolean unfinished) {
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
    protected void releaseGesture(ReplayModel.ReplayActionModel value) {
        //这里不需要处理,因为构建的手势派发后由系统去决定是否终止
        super.releaseGesture(value);
    }

    @Override
    protected void releaseKey(ReplayModel.ReplayActionModel value) {
        //发送抬起事件
        service.performGlobalAction(value.getCode());
        super.releaseKey(value);
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
        return service.dispatchGesture(payload.builder.build(), null, new Handler(Looper.getMainLooper()));
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

    public static class GesturePayload extends AbstractPayload {
        private boolean empty;
        private final GestureDescription.Builder builder;
        private ReplayModel.ReplayActionModel root;

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

    public static class KeyEventPayload extends AbstractPayload {
        private boolean empty;
        private final List<KeyEvent> events;
        private ReplayModel.ReplayActionModel root;

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
