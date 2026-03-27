package pub.carzy.auto_script.core.sub;

import android.util.Log;

import java.util.HashSet;
import java.util.LinkedHashSet;
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
import pub.carzy.auto_script.core.data.ReplayModel;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 简单的回放
 *
 * @author admin
 */
public abstract class AbstractReplay<T extends Replay.Payload, D extends Replay.Payload> implements Replay {
    /**
     * 播放状态
     */
    protected final AtomicInteger status = new AtomicInteger(STOP);
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
    protected AtomicLong tick = new AtomicLong(10);
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

    @Override
    public void clearCallback() {
        callback.clear();
    }

    @Override
    public void addCallback(ResultListener listener) {
        callback.add(listener);
    }

    @Override
    public void setRepeatCount(int repeatCount) {
        this.repeatCount.set(repeatCount);
    }

    /**
     * 手势回调,记录手势执行或被取消,打印日志
     */
    public AbstractReplay() {
        //单线程池
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 启动播放
     */
    @Override
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
            synchronized (this) {
                model.recover();
            }
            doSelfInit();
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

    protected void doSelfInit() {

    }

    @Override
    public int getStatus() {
        return status.get();
    }

    /**
     * 停止
     */
    @Override
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

    @Override
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
     * 释放(抬起)已经按下的手势或按键
     */
    private synchronized void releaseKeyMap() {
        //获取正在处理的数据
        ConcurrentNavigableMap<Long, ReplayModel.ReplayActionModel> waitMap = model.headWaitMap(System.currentTimeMillis() - startTime.get(), true);
        ThreadUtil.runOnUi(() -> {
            if (waitMap.isEmpty()) {
                return;
            }
            //释放已经按下的键
            waitMap.forEach((key, value) -> {
                while (value != null) {
                    //全部类型都抬起
                    if (value.getType() == ScriptActionEntity.KEY_EVENT && value.getCode() != null) {
                        long r = value.getRemainingTime().get();
                        //小于0说明已经被释放了,大于等于时长说明未开始
                        if (r >= value.getDuration() && r <= 0) {
                            continue;
                        }
                        //这里直接调用子类处理方法
                        releaseKey(value);
                    } else if (value.getType() == ScriptActionEntity.GESTURE) {
                        AtomicInteger current = value.getCurrent();
                        //说明已经执行过了或未执行过
                        if (current.get() >= value.getPoints().size() || current.get() == 0) {
                            return;
                        }
                        releaseGesture(value);
                    }
                    //递归处理
                    value = value.getLast();
                }
            });
        });
    }

    protected void releaseGesture(ReplayModel.ReplayActionModel value) {
        value.getCurrent().set(value.getPoints().size());
    }

    protected void releaseKey(ReplayModel.ReplayActionModel value) {
        value.getRemainingTime().set(0);
    }

    /**
     * 恢复
     */
    @Override
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
    protected void tickProcess() {
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
            T gesturePayload = createGesturePayload();
            D eventPayload = createKeyEventPayload();
            //按键时间
            //是否完成,递归判断,只有存在一个未完成就不能从wait中移除
            AtomicBoolean unfinished = new AtomicBoolean(false);
            //由于要递归处理,所以使用回调方式
            Consumer<ReplayModel.ReplayActionModel> consumer = replayActionModel -> {
                if (replayActionModel.getType() == ScriptActionEntity.GESTURE) {
                    try {
                        //处理手势
                        processGestureAction(gesturePayload, replayActionModel, unfinished);
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                    }
                } else if (replayActionModel.getType() == ScriptActionEntity.KEY_EVENT) {
                    try {
                        //处理键类型
                        processCodeAction(eventPayload, replayActionModel, unfinished);
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "tickProcess exception", e);
                    }
                }
            };
            synchronized (this) {
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
            }
            if (!gesturePayload.isEmpty()) {
                //发送手势
                ThreadUtil.runOnUi(() -> {
                    if (status.get() != RUNNING) {
                        return;
                    }
                    if (!dispatchGesture(gesturePayload)) {
                        Log.d(this.getClass().getCanonicalName(), "dispatchGesture fail");
                    }
                });
            }
            if (!eventPayload.isEmpty()) {
                //发送键事件
                ThreadUtil.runOnUi(() -> {
                    if (status.get() != RUNNING) {
                        return;
                    }
                    if (!dispatchKeyEvent(eventPayload)) {
                        Log.w(this.getClass().getCanonicalName(), "performGlobalAction failure");
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
                        if (repeatCount.get() > 0) {
                            repeatCount.set(repeatCount.get() - 1);
                        }
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

    protected abstract D createKeyEventPayload();

    protected abstract boolean dispatchKeyEvent(D item);

    protected abstract boolean dispatchGesture(T payload);

    protected abstract void processGestureAction(T payload, ReplayModel.ReplayActionModel replayActionModel, AtomicBoolean unfinished);

    protected abstract T createGesturePayload();

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
    protected abstract void processCodeAction(D keyEvents, ReplayModel.ReplayActionModel model, AtomicBoolean unfinished);

    @Override
    public void clear() {
        stop();
        model = null;
        callback.clear();
    }

    @Override
    public void close() {
        clear();
    }

}
