package pub.carzy.auto_script.service.sub;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.data.ReplayModel;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 简单的回放
 *
 * @author admin
 */
public class SimpleReplay {
    /**
     * 无障碍
     */
    private final MyAccessibilityService service;
    /**
     * 播放状态
     */
    private final AtomicInteger status = new AtomicInteger(STOP);
    /**
     * 运行中
     */
    public static final int RUNNING = 0;
    /**
     * 暂停
     */
    public static final int PAUSE = -1;
    /**
     * 停止
     */
    public static final int STOP = -2;
    /**
     * 数据
     */
    @Getter
    @Setter
    private ReplayModel model;
    /**
     * 定时任务线程池
     */
    private final ScheduledExecutorService scheduler;
    /**
     * 时间片
     */
    @Getter
    @Setter
    private AtomicLong tick = new AtomicLong(10);
    /**
     * 开始时间,开始时这个时间等于当前时间,,恢复时startTime += 当前时间-暂停时间
     */
    private final AtomicLong startTime = new AtomicLong(0);
    /**
     * 暂停时间,暂停时这个时间等于当前时间
     */
    private final AtomicLong pauseTime = new AtomicLong(0);
    /**
     * 重复次数 <=0时终止
     */
    private final AtomicInteger repeatCount = new AtomicInteger(-1);
    /**
     * 回调
     */
    private final Set<ResultListener> callback = new LinkedHashSet<>();

    public void clearCallback() {
        callback.clear();
    }

    public void addCallback(ResultListener listener) {
        callback.add(listener);
    }

    public void removeCallback(ResultListener listener) {
        callback.remove(listener);
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount.set(repeatCount);
    }

    public int getRepeatCount() {
        return repeatCount.get();
    }

    /**
     * 手势回调,记录手势执行或被取消,打印日志
     */
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
        this.service = service;
        //单线程池
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 启动播放
     */
    public void start() {
        //如果重复次数小于等于0则退出
        if (repeatCount.get() == 0) {
            //调用start失败回调
            callback.forEach(c -> c.start(ResultListener.FAIL, null, null));
            return;
        }
        try {
            //将标志位恢复成运行状态
            status.set(RUNNING);
            //重置或初始化
            if (model.getInited()) {
                model.recover();
            } else {
                model.init();
            }
            //记录开始时间 加上延迟时间
            startTime.set(System.currentTimeMillis() + model.getDelayStart());
            //调用时间片任务
            scheduler.schedule(this::tickProcess, 0, TimeUnit.MILLISECONDS);
            //调用成功回调
            callback.forEach(c -> c.start(ResultListener.SUCCESS, null, null));
        } catch (Exception e) {
            //调用失败回调
            callback.forEach(c -> c.start(ResultListener.EXCEPTION, null, e));
        }
    }

    public int getStatus() {
        return status.get();
    }

    /**
     * 停止
     */
    public void stop() {
        try {
            if (status.get() == STOP) {
                //失败回调
                callback.forEach(c -> c.stop(ResultListener.FAIL, null, null));
                return;
            }
            status.set(STOP);
            //释放键类型事件
            releaseKeyMap();
            //成功回调
            callback.forEach(c -> c.stop(ResultListener.SUCCESS, null, null));
        } catch (Exception e) {
            //失败回调
            callback.forEach(c -> c.stop(ResultListener.EXCEPTION, null, e));
        }
    }

    public void pause() {
        try {
            if (status.get() != RUNNING) {
                //失败回调
                callback.forEach(c -> c.pause(ResultListener.FAIL, null, null));
                return;
            }
            status.set(PAUSE);
            //记录暂停时间
            pauseTime.set(System.currentTimeMillis());
            //释放键事件,因为暂停时有其他手势操作
            releaseKeyMap();
            //成功回调
            callback.forEach(c -> c.pause(ResultListener.SUCCESS, null, null));
        } catch (Exception e) {
            //失败回调
            callback.forEach(c -> c.pause(ResultListener.EXCEPTION, null, e));
        }
    }

    /**
     * 只有部分按键可以比如home,返回,进程,而电源键和音量加减不能正常执行
     */
    private void releaseKeyMap() {
        //获取正在处理的数据
        ConcurrentNavigableMap<Long, ReplayModel.ReplayActionModel> waitMap = model.headWaitMap(System.currentTimeMillis() - startTime.get(), true);
        ThreadUtil.runOnUi(() -> {
            if (waitMap.isEmpty()) {
                return;
            }
            //释放已经按下的键
            waitMap.forEach((key, value) -> {
                while (value != null) {
                    if (value.getType() == ScriptActionEntity.KEY_EVENT && value.getCode() != null) {
                        long r = value.getRemainingTime().get();
                        //小于0说明已经被释放了,大于等于时长说明未开始
                        if (r >= value.getDuration() && r <= 0) {
                            continue;
                        }
                        //释放
                        if (!service.performGlobalAction(value.getCode())) {
                            Log.w(SimpleReplay.class.getCanonicalName(), "pause#performGlobalAction: failure");
                        }
                    }
                    //递归处理
                    value = value.getLast();
                }
            });
        });
    }

    /**
     * 恢复
     */
    public void resume() {
        try {
            if (status.get() != PAUSE) {
                callback.forEach(c -> c.resume(ResultListener.FAIL, null, null));
                return;
            }
            status.set(RUNNING);
            //开始时间加上间隔时长
            startTime.set(startTime.get() + System.currentTimeMillis() - pauseTime.get());
            //调用时间片方法
            scheduler.schedule(this::tickProcess, 0, TimeUnit.MILLISECONDS);
            callback.forEach(c -> c.resume(ResultListener.SUCCESS, null, null));
        } catch (Exception e) {
            callback.forEach(c -> c.resume(ResultListener.EXCEPTION, null, e));
        }
    }

    /**
     * 时间片任务
     */
    private void tickProcess() {
        if (status.get() != RUNNING) {
            return;
        }
        //间隔时长
        long duration = System.currentTimeMillis() - startTime.get();
        //获取可执行列表
        ConcurrentNavigableMap<Long, ReplayModel.ReplayActionModel> headWaitMap = model.headWaitMap(duration, true);
        if (status.get() != RUNNING) {
            return;
        }
        if (!headWaitMap.isEmpty()) {
            //手势构造器
            GestureDescription.Builder builder = new GestureDescription.Builder();
            //由于builder没有方法可以判断是否有手势操作,所以需要一个状态位来标识
            AtomicBoolean hasGesture = new AtomicBoolean(false);
            //按键时间
            List<KeyEvent> keyEvents = new ArrayList<>();
            //是否完成,递归判断,只有存在一个未完成就不能从wait中移除
            AtomicBoolean unfinished = new AtomicBoolean(false);
            //由于要递归处理,所以使用回调方式
            Consumer<ReplayModel.ReplayActionModel> consumer = replayActionModel -> {
                if (replayActionModel.getType() == ScriptActionEntity.GESTURE) {
                    try {
                        //处理手势
                        processGestureAction(builder, replayActionModel, unfinished, hasGesture);
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                    }
                } else if (replayActionModel.getType() == ScriptActionEntity.KEY_EVENT) {
                    try {
                        //处理键类型
                        processCodeAction(replayActionModel, keyEvents, unfinished);
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                    }
                }
            };
            //删除的id
            Set<Long> ids = new HashSet<>();
            for (Map.Entry<Long, ReplayModel.ReplayActionModel> line : headWaitMap.entrySet()) {
                ReplayModel.ReplayActionModel actionModel = line.getValue();
                Long id = line.getKey();
                //回调执行当前action
                try {
                    consumer.accept(actionModel);
                } catch (Exception e) {
                    Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                }
                //遍历action next链表,递归处理
                ReplayModel.ReplayActionModel loop = actionModel.getLast();
                while (loop != null) {
                    try {
                        consumer.accept(loop);
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "while tickProcess exception", e);
                    }
                    loop = loop.getLast();
                }
                //已完成则添加到移除列表
                if (!unfinished.get()) {
                    ids.add(id);
                }
            }
            //将指定的id移到删除map
            model.removeToDeleteMap(ids);
            //发送手势
            if (hasGesture.get()) {
                ThreadUtil.runOnUi(() -> {
                    if (service.dispatchGesture(builder.build(), gestureCallback, null)) {
                        Log.d(this.getClass().getCanonicalName(), "dispatchGesture success");
                    } else {
                        Log.d(this.getClass().getCanonicalName(), "dispatchGesture fail");
                    }
                });
            }
            //发送键事件
            if (!keyEvents.isEmpty()) {
                ThreadUtil.runOnUi(() -> {
                    for (KeyEvent item : keyEvents) {
                        if (!service.performGlobalAction(item.getKeyCode())) {
                            Log.w(this.getClass().getCanonicalName(), "performGlobalAction failure");
                        }
                    }
                });
            }
        }
        if (status.get() != RUNNING) {
            return;
        }
        //action全部处理完成,进行后续处理
        if (model.getActionWaitMap().isEmpty()) {
            //延迟结束
            if (model.getDelayEndCount().get() > 0) {
                model.getDelayEndCount().set(model.getDelayEndCount().get() - tick.get());
            } else {
                //完成回调
                this.callback.forEach(completedListener -> completedListener.before(status.get(), repeatCount.get()));
                try {
                    //小于等于0则退出
                    boolean out = repeatCount.get() == 0;
                    if (!out) {
                        repeatCount.set(repeatCount.get() - 1);
                        out = repeatCount.get() == 0;
                    }
                    if (out) {
                        //进入停止状态,并终止后续任务
                        stop();
                        return;
                    }
                    //重置
                    recover();
                } finally {
                    //回调
                    this.callback.forEach(completedListener -> completedListener.after(status.get(), repeatCount.get()));
                }
            }
        }
        //提交下一个时间片任务
        scheduler.schedule(this::tickProcess, tick.get(), TimeUnit.MILLISECONDS);
    }

    private void recover() {
        //重置
        model.recover();
        //重置开始时间
        startTime.set(System.currentTimeMillis());
    }

    /**
     * 处理键类型
     *
     * @param model      action
     * @param keyEvents  事件集合容器
     * @param unfinished 是否完成
     */
    private void processCodeAction(ReplayModel.ReplayActionModel model, List<KeyEvent> keyEvents, AtomicBoolean unfinished) {
        if (model == null || model.getCode() == null || model.getRemainingTime().get() <= 0) {
            return;
        }
        //如果剩余时间等于时长说明未开始,则需要准备按下事件
        if (model.getRemainingTime().get() == model.getDuration()) {
            keyEvents.add(new KeyEvent(KeyEvent.ACTION_DOWN, model.getCode()));
        }
        //扣除剩余时间
        model.getRemainingTime().set(model.getRemainingTime().get() - tick.get());
        //剩余时间等于小于0说明时间片用完了,可以发送抬起事件
        if (model.getRemainingTime().get() <= 0) {
            keyEvents.add(new KeyEvent(KeyEvent.ACTION_UP, model.getCode()));
        } else {
            //还有剩余时间就标记成未完成,防止被移除
            unfinished.set(true);
        }
    }

    /**
     * 处理手势action
     *
     * @param builder    手势构造器
     * @param action     action
     * @param unfinished 未完成
     * @param hasGesture 是否存在手势
     */
    private void processGestureAction(GestureDescription.Builder builder, ReplayModel.ReplayActionModel action, AtomicBoolean unfinished, AtomicBoolean hasGesture) {
        if (action == null) {
            return;
        }
        //查看索引位
        int current = action.getCurrent().get();
        //已处理
        if (current >= action.getPoints().size()) {
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
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, Math.max(1, action.getDuration())));
        //将current设置为size来标记当前已被处理
        action.getCurrent().set(action.getPoints().size());
        //设置存在手势
        hasGesture.set(true);
    }

    public void clear() {
        model = null;
        status.set(STOP);
        callback.clear();
    }

    public interface ResultListener {
        int SUCCESS = 0;
        int FAIL = -1;
        int EXCEPTION = -2;

        default void stop(int code, String message, Exception e) {

        }

        default void pause(int code, String message, Exception e) {

        }

        default void resume(int code, String message, Exception e) {

        }

        default void start(int code, String message, Exception e) {

        }

        default void before(int status, int count) {

        }

        default void after(int status, int count) {

        }
    }
}
