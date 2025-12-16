package pub.carzy.auto_script.service.data;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class SimpleReplay {
    private final MyAccessibilityService service;
    private List<ScriptActionEntity> actions;
    private List<ScriptPointEntity> points;

    private final AtomicInteger status = new AtomicInteger(STOP);
    public static final int RUNNING = 0;
    public static final int PAUSE = -1;
    public static final int STOP = -2;
    private final AtomicBoolean needBuild = new AtomicBoolean(true);
    private final ConcurrentSkipListMap<Long, Object> actionMap = new ConcurrentSkipListMap<>();
    private final Map<Long, Set<ScriptPointEntity>> pointMap = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Long, ScriptActionEntity> keyMap = new ConcurrentSkipListMap<>();
    /**
     * store executed actions, so resume() does not rebuild from raw list
     */
    private final ConcurrentSkipListMap<Long, Object> removeMap = new ConcurrentSkipListMap<>();
    private final ScheduledExecutorService scheduler;
    @Getter
    @Setter
    private AtomicLong tick = new AtomicLong(10);
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong pauseTime = new AtomicLong(0);
    private final ControllerCallback<Integer> defaultCallback = callback -> {
    };
    private final AccessibilityService.GestureResultCallback gestureCallback = new AccessibilityService.GestureResultCallback() {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            super.onCompleted(gestureDescription);
            Log.d(SimpleReplay.this.getClass().getCanonicalName(), "GestureResultCallback#onCompleted: ");
        }

        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            super.onCancelled(gestureDescription);
            Log.d(SimpleReplay.this.getClass().getCanonicalName(), "GestureResultCallback#onCancelled: ");
        }
    };

    public SimpleReplay(MyAccessibilityService service) {
        this(service, null, null);
    }

    public SimpleReplay(MyAccessibilityService service, List<ScriptActionEntity> actions, List<ScriptPointEntity> points) {
        this.service = service;
        this.actions = actions;
        this.points = points;
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void setActions(List<ScriptActionEntity> actions) {
        this.actions = actions;
        needBuild.set(true);
    }

    public void setPoints(List<ScriptPointEntity> points) {
        this.points = points;
        needBuild.set(true);
    }

    public void start(ControllerCallback<Integer> callback) {
        try {
            //判断是否处于暂停状态,如果是暂停状态需要提示用户是否丢弃已经运行的脚本 todo
            //将标志位恢复成运行状态,并重新构建列表
            status.set(RUNNING);
            buildData();
            scheduler.schedule(() -> tickProcess(callback), 0, TimeUnit.MILLISECONDS);
            startTime.set(System.currentTimeMillis());
        } catch (Exception e) {
            callback.catchMethod(e);
        } finally {
            callback.finallyMethod();
        }
    }

    public int getStatus() {
        return status.get();
    }

    private void buildData() {
        if (needBuild.get()) {
            if (actions == null || points == null) {
                throw new NullPointerException("actions or points is null");
            }
            actionMap.clear();
            pointMap.clear();
            removeMap.clear();
            //转换
            Map<Long, Set<ScriptActionEntity>> map = actions.stream()
                    .sorted(Comparator.comparingLong(ScriptActionEntity::getDownTime))
                    .collect(Collectors.groupingBy(ScriptActionEntity::getDownTime, Collectors.toSet()));
            for (Map.Entry<Long, Set<ScriptActionEntity>> item : map.entrySet()) {
                actionMap.put(item.getKey(), item.getValue().size() == 1 ? item.getValue().iterator().next()
                        : item.getValue());
            }
            pointMap.putAll(points.stream()
                    .sorted(Comparator.comparingLong(ScriptPointEntity::getTime))
                    .collect(Collectors.groupingBy(ScriptPointEntity::getParentId, Collectors.toCollection(LinkedHashSet::new))));
            needBuild.set(false);
        } else {
            //重置map
            Map<Long, Object> temp = new LinkedHashMap<>(actionMap.size() + pointMap.size());
            temp.putAll(removeMap);
            temp.putAll(actionMap);
            removeMap.clear();
            actionMap.clear();
            actionMap.putAll(temp);
        }
    }

    public void stop(ControllerCallback<Integer> callback) {
        try {
            if (status.get() == STOP) {
                return;
            }
            status.set(STOP);
            releaseKeyMap();
            callback.complete(status.get());
        } catch (Exception e) {
            callback.catchMethod(e);
        } finally {
            callback.finallyMethod();
        }
    }

    public void pause(ControllerCallback<Integer> callback) {
        try {
            if (status.get() != RUNNING) {
                return;
            }
            status.set(PAUSE);
            pauseTime.set(System.currentTimeMillis());
            releaseKeyMap();
            callback.complete(status.get());
        } catch (Exception e) {
            callback.catchMethod(e);
        } finally {
            callback.finallyMethod();
        }
    }

    /**
     * 只有部分按键可以比如home,返回,进程,而电源键和音量加减不能正常执行
     */
    private void releaseKeyMap() {
        //这里还得释放模拟按键
        ThreadUtil.runOnUi(() -> {
            if (keyMap.isEmpty()) {
                return;
            }
            Map<Long, ScriptActionEntity> map = new LinkedHashMap<>(keyMap);
            keyMap.clear();
            map.forEach((key, value) -> {
                if (value == null || value.getCode() == null) {
                    return;
                }
                if (!service.performGlobalAction(value.getCode())) {
                    Log.w(SimpleReplay.class.getCanonicalName(), "pause#performGlobalAction: failure");
                }
            });
        });
    }

    public void resume(ControllerCallback<Integer> callback) {
        try {
            if (status.get() != PAUSE) {
                return;
            }
            status.set(RUNNING);
            startTime.set(startTime.get() + System.currentTimeMillis() - pauseTime.get());
            scheduler.schedule(() -> tickProcess(callback), 0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            callback.catchMethod(e);
        } finally {
            callback.finallyMethod();
        }
    }

    public void start() {
        start(defaultCallback);
    }

    public void stop() {
        stop(defaultCallback);
    }

    public void pause() {
        pause(defaultCallback);
    }

    public void resume() {
        resume(defaultCallback);
    }

    @SuppressWarnings("unchecked")
    private void tickProcess(ControllerCallback<Integer> callback) {
        if (status.get() != RUNNING) {
            return;
        }
        //间隔时长
        long duration = System.currentTimeMillis() - startTime.get();
        //视图map
        NavigableMap<Long, Object> readyMap = actionMap.headMap(duration, true);
        List<Object> actions = new ArrayList<>(readyMap.values());
        if (status.get() != RUNNING) {
            return;
        }
        removeMap.putAll(readyMap);
        readyMap.clear();
        GestureDescription.Builder builder = new GestureDescription.Builder();
        AtomicBoolean hasGesture = new AtomicBoolean(false);
        List<KeyEvent> keyEvents = new ArrayList<>();
        BiConsumer<ScriptActionEntity, Boolean> consumer = (item, isUp) -> {
            if (item == null) {
                return;
            }
            if (item.getType() == ScriptActionEntity.GESTURE) {
                try {
                    hasGesture.set(true);
                    dispatchGesture(builder, item);
                } catch (Exception e) {
                    Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                }
            } else if (item.getType() == ScriptActionEntity.KEY_EVENT) {
                try {
                    dispatchKeyEvent(item, keyEvents, isUp);
                } catch (Exception e) {
                    Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                }
            }
        };
        //先执行未处理完的
        ConcurrentNavigableMap<Long, ScriptActionEntity> headKeyMap = keyMap.headMap(duration, true);
        headKeyMap.forEach((id, item) -> consumer.accept(item, true));
        headKeyMap.clear();
        actions.forEach(action -> {
            if (single(action)) {
                consumer.accept((ScriptActionEntity) action, false);
            } else {
                ((Set<ScriptActionEntity>) action).forEach(item -> consumer.accept(item, false));
            }
        });
        if (!actions.isEmpty() && hasGesture.get()) {
            ThreadUtil.runOnUi(() -> {
                if (service.dispatchGesture(builder.build(), gestureCallback, null)) {
                    Log.d(this.getClass().getCanonicalName(), "dispatchGesture success");
                } else {
                    Log.d(this.getClass().getCanonicalName(), "dispatchGesture fail");
                }
            });
        }
        if (!keyEvents.isEmpty()) {
            ThreadUtil.runOnUi(() -> {
                for (KeyEvent item : keyEvents) {
                    if (!service.performGlobalAction(item.getKeyCode())) {
                        Log.w(this.getClass().getCanonicalName(), "performGlobalAction failure");
                    }
                }
            });
        }
        if (status.get() != RUNNING) {
            return;
        }
        //没有就结束
        if (actionMap.isEmpty()) {
            stop(callback);
            return;
        }
        scheduler.schedule(() -> tickProcess(callback), tick.get(), TimeUnit.MILLISECONDS);
    }

    private void dispatchKeyEvent(ScriptActionEntity a, List<KeyEvent> keyEvents, boolean isUp) {
        if (a == null || a.getCode() == null) {
            return;
        }
        if (!isUp) {
            keyEvents.add(new KeyEvent(KeyEvent.ACTION_DOWN, a.getCode()));
            keyMap.put(a.getUpTime(), a);
        }
        if (a.getUpTime() - a.getDownTime() >= tick.get()) {
            //间隔小于时间片时，跟着发送up事件
            keyEvents.add(new KeyEvent(KeyEvent.ACTION_UP, a.getCode()));
        }
    }

    public boolean single(Object value) {
        return value instanceof ScriptActionEntity;
    }

    private void dispatchGesture(GestureDescription.Builder builder, ScriptActionEntity action) {
        if (action == null) {
            return;
        }
        //先根据action查找到所有 point
        Set<ScriptPointEntity> points = pointMap.get(action.getId());
        if (points == null || points.isEmpty()) {
            return;
        }
        boolean first = true;
        Path path = new Path();
        long time = action.getDownTime();
        for (ScriptPointEntity point : points) {
            if (first) {
                path.moveTo(point.getX(), point.getY());
                first = false;
                continue;
            }
            path.lineTo(point.getX(), point.getY());
            time = Math.max(time, point.getTime());
        }
        long duration = Math.max(1, time - action.getDownTime());
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
    }
}
