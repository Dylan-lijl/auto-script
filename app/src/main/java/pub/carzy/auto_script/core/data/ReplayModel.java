package pub.carzy.auto_script.core.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.utils.BeanHandler;

/**
 * 重放实体
 *
 * @author admin
 */
public class ReplayModel {
    //-----------重复脚本实体属性----------
    private String name;
    private Long count;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getActionCount() {
        return actionCount;
    }

    public void setActionCount(Long actionCount) {
        this.actionCount = actionCount;
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Long getDelayEnd() {
        return delayEnd;
    }

    public void setDelayEnd(Long delayEnd) {
        this.delayEnd = delayEnd;
    }

    public Long getDelayStart() {
        return delayStart;
    }

    public void setDelayStart(Long delayStart) {
        this.delayStart = delayStart;
    }

    public Boolean getInited() {
        return inited;
    }

    public void setInited(Boolean inited) {
        this.inited = inited;
    }

    public AtomicLong getDelayEndCount() {
        return delayEndCount;
    }

    public void setDelayEndCount(AtomicLong delayEndCount) {
        this.delayEndCount = delayEndCount;
    }

    public List<ReplayActionModel> getActions() {
        return actions;
    }

    public void setActions(List<ReplayActionModel> actions) {
        this.actions = actions;
    }

    public ConcurrentSkipListMap<Long, ReplayActionModel> getActionWaitMap() {
        return actionWaitMap;
    }

    public void setActionWaitMap(ConcurrentSkipListMap<Long, ReplayActionModel> actionWaitMap) {
        this.actionWaitMap = actionWaitMap;
    }

    public ConcurrentSkipListMap<Long, ReplayActionModel> getActionDeleteMap() {
        return actionDeleteMap;
    }

    public void setActionDeleteMap(ConcurrentSkipListMap<Long, ReplayActionModel> actionDeleteMap) {
        this.actionDeleteMap = actionDeleteMap;
    }

    private Long actionCount;
    private Long totalDuration;
    private Long delayEnd;
    private Long delayStart;
    private Boolean inited = false;
    private AtomicLong delayEndCount = new AtomicLong(0);
    private List<ReplayActionModel> actions = new ArrayList<>();

    //----------action属性------------------
    public static class ReplayActionModel {
        private Long id;
        private Long startTime;
        private Long duration;
        private Integer pointCount;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getStartTime() {
            return startTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public Integer getPointCount() {
            return pointCount;
        }

        public void setPointCount(Integer pointCount) {
            this.pointCount = pointCount;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public List<ReplayPointModel> getPoints() {
            return points;
        }

        public void setPoints(List<ReplayPointModel> points) {
            this.points = points;
        }

        public Integer getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(Integer trackingId) {
            this.trackingId = trackingId;
        }

        public List<ReplayActionModel> getParallel() {
            return parallel;
        }

        public void setParallel(List<ReplayActionModel> parallel) {
            this.parallel = parallel;
        }

        public AtomicInteger getCurrent() {
            return current;
        }

        public void setCurrent(AtomicInteger current) {
            this.current = current;
        }

        public AtomicLong getRemainingTime() {
            return remainingTime;
        }

        public void setRemainingTime(AtomicLong remainingTime) {
            this.remainingTime = remainingTime;
        }

        public boolean isMerged() {
            return merged;
        }

        public void setMerged(boolean merged) {
            this.merged = merged;
        }

        private Integer type;
        private Integer code;
        private Integer index;
        private List<ReplayPointModel> points = new ArrayList<>();
        //----------运行数据------------
        private Integer trackingId;
        //如果时间相同,则形成链表
        private List<ReplayActionModel> parallel = new ArrayList<>();
        //当前运行到哪一步,手势类型可以用的到
        private AtomicInteger current = new AtomicInteger(0);
        //当前剩余时间,键类型可以用的到
        private AtomicLong remainingTime = new AtomicLong(0);
        private boolean merged = false;

        /**
         * 重置
         */
        public void reset() {
            current.set(0);
            trackingId = null;
            remainingTime.set(duration);
            for (ReplayActionModel loop : parallel) {
                loop.reset();
            }
            for (ReplayPointModel point : points) {
                point.reset();
            }
        }

        public ReplayPointModel getLastPoint() {
            if (!points.isEmpty()) {
                return points.get(points.size() - 1);
            }
            return null;
        }
    }

    //-----------point属性------------------
    public static class ReplayPointModel {
        private Long id;
        private Float x;
        private Float y;
        private Long deltaTime;
        private Float order;
        //-------------运行时数据-------------
        //这个参数主要解决tick小于delta时间
        private final AtomicLong remainingTime;
        boolean dispatched;

        public ReplayPointModel(Long id, Float x, Float y, Long deltaTime, Float order) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.deltaTime = deltaTime;
            this.order = order;
            remainingTime = new AtomicLong(deltaTime);
        }

        public void reset() {
            remainingTime.set(deltaTime);
            dispatched = false;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Float getX() {
            return x;
        }

        public void setX(Float x) {
            this.x = x;
        }

        public Float getY() {
            return y;
        }

        public void setY(Float y) {
            this.y = y;
        }

        public Long getDeltaTime() {
            return deltaTime;
        }

        public void setDeltaTime(Long deltaTime) {
            this.deltaTime = deltaTime;
        }

        public Float getOrder() {
            return order;
        }

        public void setOrder(Float order) {
            this.order = order;
        }

        public AtomicLong getRemainingTime() {
            return remainingTime;
        }

        public boolean isDispatched() {
            return dispatched;
        }

        public void setDispatched(boolean dispatched) {
            this.dispatched = dispatched;
        }
    }

    //-----------运行时数据-----------
    private ConcurrentSkipListMap<Long, ReplayActionModel> actionWaitMap = new ConcurrentSkipListMap<>();
    private ConcurrentSkipListMap<Long, ReplayActionModel> actionDeleteMap = new ConcurrentSkipListMap<>();

    public ConcurrentNavigableMap<Long, ReplayActionModel> headWaitMap(long duration, boolean b) {
        return actionWaitMap.headMap(duration, b);
    }

    public void removeToDeleteMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            ReplayActionModel model = actionWaitMap.remove(id);
            if (model == null) {
                continue;
            }
            model.reset();
            actionDeleteMap.put(id, model);
        }
    }

    public void init() {
        if (inited) {
            return;
        }
        if (actions == null || actions.isEmpty()) {
            return;
        }
        //排序 先根据startTime进行排序,如果相同则使用id进行排序
        actions.sort(Comparator.comparingLong(ReplayActionModel::getStartTime).thenComparingLong(ReplayActionModel::getId));
        //进行遍历
        for (ReplayActionModel line : actions) {
            ReplayActionModel model = actionWaitMap.get(line.getStartTime());
            if (model == null) {
                actionWaitMap.put(line.getStartTime(), line);
            } else {
                //存在说明是相同的时间,则添加到集合
                model.parallel.add(line);
            }
            //重置
            line.reset();
            //根据oder字段进行排序,如果order相同则根据id排序
            line.getPoints().sort(Comparator.comparingDouble(ReplayPointModel::getOrder).thenComparingLong(ReplayPointModel::getId));
        }
        delayEndCount.set(delayEnd);
        inited = true;
    }

    public void recover() {
        if (!inited) {
            init();
            return;
        }
        //将remove和wait组合起来
        Map<Long, ReplayActionModel> map = new LinkedHashMap<>(actionWaitMap.size() + actionDeleteMap.size());
        map.putAll(actionDeleteMap);
        map.putAll(actionWaitMap);
        //保险起见,遍历元素调用reset方法
        map.values().forEach((v) -> v.parallel.forEach(ReplayActionModel::reset));
        //清空数据
        actionWaitMap.clear();
        actionDeleteMap.clear();
        //再将数据放入wait中
        actionWaitMap.putAll(map);
        delayEndCount.set(delayEnd);
    }

    /**
     * 将数据库实体转换成数据模型
     *
     * @param root           根节点
     * @param actionEntities action
     * @param points         point
     * @return 数据模型
     */
    public static ReplayModel create(ScriptEntity root, List<ScriptActionEntity> actionEntities, List<ScriptPointEntity> points) {
        if (root == null || actionEntities == null || actionEntities.isEmpty()) {
            return null;
        }
        ReplayModel model = new ReplayModel();
        BeanHandler.copyProperties(root, model);
        Map<Long, List<ScriptPointEntity>> map = new LinkedHashMap<>();
        if (points != null && !points.isEmpty()) {
            map.putAll(points.stream().collect(Collectors.groupingBy(ScriptPointEntity::getActionId)));
        }
        for (ScriptActionEntity action : actionEntities) {
            ReplayActionModel actionModel = new ReplayActionModel();
            BeanHandler.copyProperties(action, actionModel);
            model.getActions().add(actionModel);
            List<ScriptPointEntity> list = map.get(action.getId());
            if (list != null && !list.isEmpty()) {
                for (ScriptPointEntity point : list) {
                    ReplayPointModel pointModel = new ReplayPointModel(point.getId(), point.getX(), point.getY(), point.getDeltaTime(), point.getOrder());
                    actionModel.getPoints().add(pointModel);
                }
            }
        }
        return model;
    }
}
