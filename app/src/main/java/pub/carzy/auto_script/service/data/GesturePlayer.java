package pub.carzy.auto_script.service.data;

/**
 * @author admin
 */

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;

/**
 * GesturePlayer - tick-based multi-finger player using API26+ continueStroke.
 * <p>
 * Usage:
 * GesturePlayer gp = new GesturePlayer(myAccessibilityService);
 * gp.setActions(actionsList); // List<ScriptActionEntity>
 * gp.setPoints(pointsList);   // List<ScriptPointEntity>
 * gp.start(true); // true = align to now (play immediately)
 * gp.pause();
 * gp.resume();
 * gp.stop();
 * <p>
 * Notes:
 * - ScriptPointEntity.time is expected to be absolute or relative to same baseline as action.downTime.
 * This implementation treats point.time and action.downTime as in the same time base (ms).
 *
 * @author admin
 */
public class GesturePlayer {

    private static final String TAG = "GesturePlayer";

    private final AccessibilityService service;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Configurable tick in ms
    private final long tickMs;

    // Input data
    private List<ScriptActionEntity> actions = new ArrayList<>();
    private List<ScriptPointEntity> points = new ArrayList<>();

    // Derived runtime structures:
    // actionId -> queue of points sorted ascending by time
    private final Map<Long, Deque<ScriptPointEntity>> pointsByAction = new LinkedHashMap<>();
    // actionId -> current StrokeDescription (the last stroke object in chain)
    private final Map<Long, GestureDescription.StrokeDescription> lastStrokeByAction = new HashMap<>();
    // actionId -> whether the action currently has an outstanding segment dispatched (awaiting completion)
    private final Map<Long, AtomicBoolean> busyByAction = new HashMap<>();
    // actionId -> last known coordinates (used when no point in tick to hold)
    private final Map<Long, float[]> lastPosByAction = new HashMap<>();

    // Playback control
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile boolean stopped = false;
    /**
     * System.currentTimeMillis when playback started
     */
    private long playbackStartRealMs = 0L;
    /**
     * baseline time (min downTime)
     */
    private long globalStartTime = 0L;
    private final Object lock = new Object();

    public GesturePlayer(AccessibilityService service) {
        this(service, 10);
    }

    public GesturePlayer(AccessibilityService service, long tickMs) {
        this.service = service;
        this.tickMs = Math.max(1, tickMs);
    }

    /**
     * Provide actions input.
     * Each ScriptActionEntity must contain id, downTime, upTime (or maxTime).
     */
    public void setActions(List<ScriptActionEntity> actions) {
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }

    /**
     * Provide points input.
     * Each ScriptPointEntity must contain parentId, x, y, time.
     */
    public void setPoints(List<ScriptPointEntity> points) {
        this.points = points == null ? new ArrayList<>() : new ArrayList<>(points);
    }

    /**
     * Prepare internal maps. Call after setActions/setPoints and before start().
     */
    public void prepare() {
        pointsByAction.clear();
        lastStrokeByAction.clear();
        busyByAction.clear();
        lastPosByAction.clear();

        // group points by parentId and sort ascending by time
        Map<Long, List<ScriptPointEntity>> tmp = new LinkedHashMap<>();
        for (ScriptPointEntity p : points) {
            if (p.getParentId() == null) continue;
            tmp.computeIfAbsent(p.getParentId(), k -> new ArrayList<>()).add(p);
        }
        for (Map.Entry<Long, List<ScriptPointEntity>> e : tmp.entrySet()) {
            List<ScriptPointEntity> list = e.getValue();
            list.sort(Comparator.comparingLong(ScriptPointEntity::getTime));
            Deque<ScriptPointEntity> dq = new LinkedList<>(list);
            pointsByAction.put(e.getKey(), dq);
        }

        // init busy flags and last pos by available points if any
        for (ScriptActionEntity a : actions) {
            long id = a.getId();
            busyByAction.put(id, new AtomicBoolean(false));
            Deque<ScriptPointEntity> dq = pointsByAction.get(id);
            if (dq != null && !dq.isEmpty()) {
                ScriptPointEntity first = dq.peekFirst();
                lastPosByAction.put(id, new float[]{first.getX(), first.getY()});
            } else {
                lastPosByAction.put(id, new float[]{0f, 0f});
            }
        }

        // compute globalStartTime (min downTime)
        long min = Long.MAX_VALUE;
        for (ScriptActionEntity a : actions) {
            if (a.getDownTime() != null && a.getDownTime() < min) min = a.getDownTime();
        }
        if (min == Long.MAX_VALUE) min = System.currentTimeMillis();
        globalStartTime = min;
    }

    /**
     * Start playback. If alignToNow == true, map globalStart to now (so playback starts immediately).
     */
    public void start(boolean alignToNow) {
        if (running) {
            Log.w(TAG, "already running");
            return;
        }
        prepare();
        running = true;
        paused = false;
        stopped = false;
        // playbackStartRealMs mapping:
        if (alignToNow) {
            playbackStartRealMs = System.currentTimeMillis();
        } else {
            // keep baseline so that virtual time = globalStartTime + (now - playbackStartRealMs)
            playbackStartRealMs = System.currentTimeMillis() - (globalStartTime - globalStartTime);
        }
        scheduler.scheduleAtFixedRate(this::tickLoop, 0, tickMs, TimeUnit.MILLISECONDS);
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        if (!running) return;
        paused = false;
    }

    /**
     * Stop playback and try to finish (send final UP for active strokes).
     */
    public void stop() {
        stopped = true;
        running = false;
        // attempt to gracefully end: for each action that has lastStroke, create a final continueStroke with willContinue=false
        try {
            dispatchFinalUpForAll();
        } catch (Exception ex) {
            Log.e(TAG, "stop error", ex);
        } finally {
            scheduler.shutdownNow();
        }
    }

    private void dispatchFinalUpForAll() throws InterruptedException {
        // Build strokes for all actions that are currently active (lastStroke exists)
        GestureDescription.Builder gb = new GestureDescription.Builder();
        boolean any = false;
        for (Map.Entry<Long, GestureDescription.StrokeDescription> entry : lastStrokeByAction.entrySet()) {
            long aid = entry.getKey();
            GestureDescription.StrokeDescription prev = entry.getValue();
            if (prev == null) continue;
            float[] pos = lastPosByAction.getOrDefault(aid, new float[]{0f, 0f});
            Path p = new Path();
            p.moveTo(pos[0], pos[1]);
            // create a final small segment and mark willContinue = false
            GestureDescription.StrokeDescription next = prev.continueStroke(p, 0, Math.max(1, tickMs), false);
            gb.addStroke(next);
            any = true;
        }
        if (!any) return;
        CountDownLatch latch = new CountDownLatch(1);
        service.dispatchGesture(gb.build(), new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                latch.countDown();
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                latch.countDown();
            }
        }, null);
        latch.await(1000, TimeUnit.MILLISECONDS);
    }

    // compute current virtual playback time in same base as action.downTime / point.time
    private long currentVirtualTimeMs() {
        long now = System.currentTimeMillis();
        // virtual = globalStartTime + (now - playbackStartRealMs)
        return globalStartTime + (now - playbackStartRealMs);
    }

    // the scheduled tick loop
    private void tickLoop() {
        if (!running || paused || stopped) return;

        long tickStartVirtual = currentVirtualTimeMs();
        long tickEndVirtual = tickStartVirtual + tickMs;

        try {
            // collect strokes for actions that are not busy and whose window intersects tick
            GestureDescription.Builder gb = new GestureDescription.Builder();
            List<Long> actionsToMarkBusy = new ArrayList<>();

            for (ScriptActionEntity action : actions) {
                Long aidLong = action.getId();
                if (aidLong == null) continue;
                long aid = aidLong;
                // skip if this action already finished (no points and upTime passed)
                long actionUp = action.getUpTime() == null ? (action.getMaxTime() == null ? Long.MAX_VALUE : action.getMaxTime()) : action.getUpTime();
                long actionDown = action.getDownTime() == null ? 0L : action.getDownTime();
                if (tickEndVirtual <= actionDown) {
                    // not started yet
                    continue;
                }
                if (tickStartVirtual >= actionUp) {
                    // already ended
                    continue;
                }
                AtomicBoolean busy = busyByAction.get(aid);
                if (busy != null && busy.get()) {
                    // previous segment still not completed for this action
                    continue;
                }

                // build a segment for this action for [tickStartVirtual, tickEndVirtual)
                StrokeSegment seg = buildSegmentForAction(action, tickStartVirtual, tickEndVirtual);
                if (seg == null) continue;

                // prepare strokeDescription: either initial or continue on previous
                GestureDescription.StrokeDescription prev = lastStrokeByAction.get(aid);
                GestureDescription.StrokeDescription nextStroke;
                if (prev == null) {
                    // initial stroke (down + short duration)
                    Path p = new Path();
                    p.moveTo(seg.startX, seg.startY);
                    p.lineTo(seg.endX, seg.endY);
                    // duration = seg.durationMs
                    nextStroke = new GestureDescription.StrokeDescription(p, 0, Math.max(1, seg.durationMs), seg.willContinue);
                } else {
                    // continue previous stroke
                    Path p = new Path();
                    p.moveTo(seg.startX, seg.startY);
                    p.lineTo(seg.endX, seg.endY);
                    nextStroke = prev.continueStroke(p, 0, Math.max(1, seg.durationMs), seg.willContinue);
                }

                // store nextStroke as the new lastStroke (even before dispatch)
                lastStrokeByAction.put(aid, nextStroke);

                // add to builder
                gb.addStroke(nextStroke);
                actionsToMarkBusy.add(aid);
            }

            if (actionsToMarkBusy.isEmpty()) {
                // nothing to dispatch this tick
                return;
            }

            // mark busy for dispatched actions and dispatch combined gesture
            for (Long aid : actionsToMarkBusy) {
                AtomicBoolean b = busyByAction.get(aid);
                if (b != null) b.set(true);
            }
            CountDownLatch latch = new CountDownLatch(1);
            service.dispatchGesture(gb.build(), new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    // on completed: clear busy flags for actions included in this dispatch
                    for (Long aid : actionsToMarkBusy) {
                        AtomicBoolean b = busyByAction.get(aid);
                        if (b != null) b.set(false);
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    for (Long aid : actionsToMarkBusy) {
                        AtomicBoolean b = busyByAction.get(aid);
                        if (b != null) b.set(false);
                    }
                    latch.countDown();
                }
            }, null);

            // wait for completion to avoid flooding dispatch queue.
            // We wait up to tickMs * 2 to be safe (non-blocking limitation).
            try {
                latch.await(tickMs * 2, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }

        } catch (Throwable t) {
            Log.e(TAG, "tickLoop error", t);
        }
    }

    // helper container for a segment
    private static class StrokeSegment {
        final float startX, startY, endX, endY;
        final long durationMs;
        final boolean willContinue;

        StrokeSegment(float sx, float sy, float ex, float ey, long dur, boolean willContinue) {
            this.startX = sx;
            this.startY = sy;
            this.endX = ex;
            this.endY = ey;
            this.durationMs = dur;
            this.willContinue = willContinue;
        }
    }

    /**
     * Build a segment for action for interval [tickStartVirtual, tickEndVirtual).
     * It consumes points from the action queue whose point.time is within this interval.
     * If no point in the interval, produce a hold-segment from last known position.
     */
    private StrokeSegment buildSegmentForAction(ScriptActionEntity action, long tickStartVirtual, long tickEndVirtual) {
        long aid = action.getId();
        Deque<ScriptPointEntity> dq = pointsByAction.get(aid);
        float[] lastPos = lastPosByAction.getOrDefault(aid, new float[]{0f, 0f});
        long actionDown = action.getDownTime() == null ? 0L : action.getDownTime();
        long actionUp = action.getUpTime() == null ? (action.getMaxTime() == null ? Long.MAX_VALUE : action.getMaxTime()) : action.getUpTime();

        // 在这个动作中计算相对窗口：我们希望在 [tickStartVirtual， tickEndVirtual“ 中 （point.time）
        // 但point.time与action.downTime在同一时间基底（用户说时间是相对于downTime的？）
        // 这里我们假设 point.time 与 action.downTime 在同一基底下是绝对的。如果你的 points.time 是相对的，可以加上 actionDown 来比较。
        // 我们会同时处理：如果 point.time < actionDown，假设它是相对的，然后加上 actionDown
        Deque<ScriptPointEntity> queue = dq;
        if (queue == null) {
            // 行动不计分：只要按住最后一个位置直到上方
            if (tickStartVirtual < actionUp) {
                long dur = Math.min(tickMs, actionUp - tickStartVirtual);
                assert lastPos != null;
                return new StrokeSegment(lastPos[0], lastPos[1], lastPos[0], lastPos[1], dur, actionUp > tickEndVirtual);
            } else {
                return null;
            }
        }

        // 在刻度窗口内找到点。我们将窗口中的最后一点作为结束点。
        ScriptPointEntity startPoint = null;
        ScriptPointEntity endPoint = null;

        // 第一个点，即 >= tickStartVirtual （或相对处理）
        //我们将从头开始迭代，当pointTime<tickEndVirtual
        long lookWindowStart = tickStartVirtual;
        long lookWindowEnd = tickEndVirtual;

        // 如果积分看起来是相对的（相比actionDown非常小），请调整：
        ScriptPointEntity peek = queue.peekFirst();
        if (peek == null) return null;
        long sampleTime = peek.getTime() == null ? 0L : peek.getTime();
        // iterate and consume any points whose absolute time < tickEndVirtual
        long durConsumed = 0L;
        long lastConsumedPointTimeAbs = -1L;
        while (!queue.isEmpty()) {
            ScriptPointEntity p = queue.peekFirst();
            assert p != null;
            long pTimeAbs = p.getTime() == null ? 0L : p.getTime();
            if (pTimeAbs < tickStartVirtual) {
                // 这点已经落后了，弹出并更新 lastPos
                queue.pollFirst();
                lastPosByAction.put(aid, new float[]{p.getX(), p.getY()});
                continue;
            }
            if (pTimeAbs >= tickEndVirtual) {
                // no more points in this tick
                break;
            }
            // this point is inside [tickStartVirtual, tickEndVirtual)
            // consume it
            ScriptPointEntity consumed = queue.pollFirst();
            if (startPoint == null) {
                // start from previous lastPos
                startPoint = consumed; // but use lastPos as start coordinate
            }
            endPoint = consumed;
            lastConsumedPointTimeAbs = pTimeAbs;
            lastPosByAction.put(aid, new float[]{consumed.getX(), consumed.getY()});
        }

        if (endPoint != null) {
            float sx = lastPos[0];
            float sy = lastPos[1];
            float ex = endPoint.getX();
            float ey = endPoint.getY();

            // duration: min(tickMs, remaining until actionUp)
            long dur = Math.min(tickMs, Math.max(1, Math.min(actionUp, tickEndVirtual) - tickStartVirtual));
            boolean willContinue = actionUp > tickEndVirtual || !queue.isEmpty();
            return new StrokeSegment(sx, sy, ex, ey, dur, willContinue);
        } else {
            // no point in this tick window -> hold last pos (or if not started yet and tick intersects down, handle initial down)
            // if tick intersects actionDown and no point exists, use lastPos (or try to create down at lastPos)
            if (tickStartVirtual <= actionDown && tickEndVirtual > actionDown) {
                // action starts in this tick; create down at lastPos (or at first point)
                float sx = lastPos[0];
                float sy = lastPos[1];
                float ex = sx;
                float ey = sy;
                long dur = Math.min(tickMs, Math.max(1, Math.min(actionUp, tickEndVirtual) - Math.max(tickStartVirtual, actionDown)));
                boolean willContinue = actionUp > tickEndVirtual || !queue.isEmpty();
                return new StrokeSegment(sx, sy, ex, ey, dur, willContinue);
            } else {
                // action active but no points in this tick -> hold
                if (tickStartVirtual < actionUp) {
                    float sx = lastPos[0];
                    float sy = lastPos[1];
                    long dur = Math.min(tickMs, Math.max(1, Math.min(actionUp, tickEndVirtual) - tickStartVirtual));
                    boolean willContinue = actionUp > tickEndVirtual || !queue.isEmpty();
                    return new StrokeSegment(sx, sy, sx, sy, dur, willContinue);
                } else {
                    return null;
                }
            }
        }
    }
}
