package pub.carzy.auto_script.utils;

/**
 * 简单计时器，支持开始、暂停、恢复、停止和重置。
 * 暂停期间时间不计入总时长。
 * @author admin
 */
public class Stopwatch {

    private enum State { STOPPED, RUNNING, PAUSED }

    private long startTimeMillis = 0;
    private long accumulatedMillis = 0;
    private State state = State.STOPPED;

    /** 开始计时（会重置之前的计时） */
    public void start() {
        startTimeMillis = System.currentTimeMillis();
        accumulatedMillis = 0;
        state = State.RUNNING;
    }

    /** 暂停计时（暂停期间时间不计入总时长） */
    public void pause() {
        if (state == State.RUNNING) {
            accumulatedMillis += System.currentTimeMillis() - startTimeMillis;
            state = State.PAUSED;
        }
    }

    /** 从暂停处恢复计时 */
    public void resume() {
        if (state == State.PAUSED) {
            startTimeMillis = System.currentTimeMillis();
            state = State.RUNNING;
        }
    }

    /** 停止计时（不可继续） */
    public void stop() {
        if (state == State.RUNNING) {
            accumulatedMillis += System.currentTimeMillis() - startTimeMillis;
        }
        state = State.STOPPED;
    }

    /** 重置计时器为初始状态 */
    public void reset() {
        accumulatedMillis = 0;
        startTimeMillis = 0;
        state = State.STOPPED;
    }

    /** 获取当前经过的毫秒数 */
    public long getElapsedMillis() {
        if (state == State.RUNNING) {
            return accumulatedMillis + (System.currentTimeMillis() - startTimeMillis);
        }
        return accumulatedMillis;
    }

    /** 当前状态 */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public boolean isStopped() {
        return state == State.STOPPED;
    }
}
