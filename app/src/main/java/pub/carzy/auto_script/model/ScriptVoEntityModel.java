package pub.carzy.auto_script.model;

import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableMap;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.utils.ObservableLinkedHashMap;

/**
 * @author admin
 */
public class ScriptVoEntityModel extends BaseObservable {
    /**
     * 总结对象
     */
    private ScriptEntity root;
    /**
     * action model
     */
    private final ObservableLinkedHashMap<Long, ScriptActionModel> actions = new ObservableLinkedHashMap<>();
    /**
     * 选中action
     */
    private final ObservableLinkedHashMap<Long, Highlight> checkedAction = new ObservableLinkedHashMap<>();
    /**
     * point
     */
    private final ObservableLinkedHashMap<Long, ScriptPointEntity> points = new ObservableLinkedHashMap<>();
    /**
     * 选中的point
     */
    private final ObservableLinkedHashMap<Long, Highlight> checkedPoint = new ObservableLinkedHashMap<>();
    /**
     * 展示的point
     */
    private final ObservableLinkedHashMap<Long, ScriptPointModel> showPoints = new ObservableLinkedHashMap<>();
    /**
     * 颜色
     */
    @Getter
    @Setter
    private List<Integer> colorsResource = new ArrayList<>();
    /**
     * 是否需要保存
     */
    private Boolean saved = false;
    /**
     * 是否是新增
     */
    private Boolean add = false;

    @Bindable
    public Boolean getAdd() {
        return add;
    }

    public void setAdd(Boolean add) {
        this.add = add;
        notifyPropertyChanged(BR.add);
    }

    public ScriptVoEntityModel() {
        //给选中action加个监听
        checkedAction.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {
            @Override
            public void onMapChanged(ObservableMap<Long, Highlight> sender, Long key) {
                addShowPointsByActionId(getLastCheckActionId(), false);
                notifyPropertyChanged(BR.lastCheckAction);
                notifyPropertyChanged(BR.lastCheckActionId);
            }
        });
        checkedPoint.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {
            @Override
            public void onMapChanged(ObservableMap<Long, Highlight> sender, Long key) {
                notifyPropertyChanged(BR.lastCheckShowPoint);
                notifyPropertyChanged(BR.lastCheckShowPointId);
            }
        });
    }


    @Bindable
    public ObservableMap<Long, Highlight> getCheckedPoint() {
        return checkedPoint;
    }

    @Bindable
    public Boolean getSaved() {
        return saved;
    }

    public void setSaved(Boolean saved) {
        this.saved = saved;
        notifyPropertyChanged(BR.saved);
    }

    @Bindable
    public ScriptEntity getRoot() {
        return root;
    }

    public void setRoot(ScriptEntity root) {
        this.root = root;
        notifyPropertyChanged(BR.root);
    }

    @Bindable
    public ObservableMap<Long, ScriptPointEntity> getPoints() {
        return points;
    }

    /**
     * 获取最后一个选中的action id
     *
     * @return action id,没有则返沪null
     */
    @Bindable
    public Long getLastCheckActionId() {
        if (checkedAction.isEmpty()) {
            return null;
        }
        Iterator<Long> iterator = checkedAction.keySet().iterator();
        Long pre = null;
        while (iterator.hasNext()) {
            pre = iterator.next();
        }
        return pre;
    }

    @Bindable
    public ScriptPointModel getLastCheckShowPoint() {
        Long id = getLastCheckShowPointId();
        if (id == null) {
            return null;
        }
        return showPoints.get(id);
    }

    @Bindable
    public Long getLastCheckShowPointId() {
        if (checkedPoint.isEmpty()) {
            return null;
        }
        Iterator<Long> iterator = checkedPoint.keySet().iterator();
        Long pre = null;
        while (iterator.hasNext()) {
            pre = iterator.next();
        }
        return pre;
    }

    @Bindable
    public ScriptActionModel getLastCheckAction() {
        Long lastCheckActionId = getLastCheckActionId();
        if (lastCheckActionId == null) {
            return null;
        }
        return actions.get(lastCheckActionId);
    }

    /**
     * 设置point
     *
     * @param points 内容
     */
    public void setPoints(Collection<ScriptPointEntity> points) {
        //清空旧数据
        this.points.clear();
        //清空已选择的point
        checkedPoint.clear();
        //清空对应的points
        actions.forEach((k, v) -> {
            v.getPointIds().clear();
        });
        if (points != null) {
            //获取最后一个选中的action id
            Long checkActionId = getLastCheckActionId();
            Map<Long, ScriptPointEntity> map = new LinkedHashMap<>();
            List<ScriptPointEntity> checkActionPoints = new ArrayList<>();
            //将point放入map
            for (ScriptPointEntity line : points) {
                map.put(line.getId(), line);
                //如果属于已选择的action则放入checkActionPoints
                if (checkActionId != null && checkActionId.compareTo(line.getActionId()) == 0) {
                    checkActionPoints.add(line);
                }
                ScriptActionModel actionModel = actions.get(line.getActionId());
                if (actionModel != null) {
                    actionModel.getPointIds().add(line.getId());
                }
            }
            this.points.putAll(map);
            if (checkActionId != null) {
                //将选中的覆盖到展示point
                setShowPoints(checkActionPoints, checkActionId);
                notifyPropertyChanged(BR.showPoints);
            }
        }
        notifyPropertyChanged(BR.checkedPoint);
        notifyPropertyChanged(BR.points);
    }

    public List<ScriptPointEntity> getPointData() {
        return new ArrayList<>(points.values());
    }

    public List<BarEntry> getShowPointBarEntries() {
        return showPoints.values().stream().map(ScriptPointModel::getBarEntry).collect(Collectors.toList());
    }

    public List<Integer> getShowPointColors() {
        return showPoints.values().stream().map(ScriptPointModel::getColor).collect(Collectors.toList());
    }

    @Bindable
    public ObservableLinkedHashMap<Long, ScriptPointModel> getShowPoints() {
        return showPoints;
    }

    private void setShowPoints(Collection<ScriptPointEntity> checkActionPoints, Long checkActionId) {
        //清空旧数据
        showPoints.clear();
        if (checkActionPoints != null) {
            //获取选中的action
            ScriptActionModel actionModel = actions.get(checkActionId);
            if (actionModel != null) {
                Map<Long, ScriptPointModel> map = new LinkedHashMap<>();
                Long start = 0L;
                //排序
                List<ScriptPointEntity> sortPoints = checkActionPoints.stream().sorted(Comparator.comparingDouble(ScriptPointEntity::getOrder).thenComparingLong(ScriptPointEntity::getId)).collect(Collectors.toList());
                for (int i = 0; i < sortPoints.size(); i++) {
                    ScriptPointEntity item = sortPoints.get(i);
                    ScriptPointModel model = createScriptPointModel(item, i, start);
                    start += item.getDeltaTime();
                    map.put(model.getKey(), model);
                }
                showPoints.putAll(map);
            }
        }
        notifyPropertyChanged(BR.showPoints);
    }

    @NonNull
    private ScriptPointModel createScriptPointModel(ScriptPointEntity item, int index, Long start) {
        ScriptPointModel model = new ScriptPointModel();
        model.setKey(item.getId());
        model.setData(item);
        model.setBarEntry(new BarEntry(index, new float[]{start, item.getDeltaTime()}, item.getId()));
        //颜色根据索引来确定
        model.setColor(getColor(index));
        return model;
    }

    @Bindable
    public ObservableMap<Long, ScriptActionModel> getActions() {
        return actions;
    }

    public List<BarEntry> getActionBarEntries() {
        return actions.values().stream().map(ScriptActionModel::getBarEntry).collect(Collectors.toList());
    }

    public List<Integer> getActionColors() {
        return actions.values().stream().map(ScriptActionModel::getColor).collect(Collectors.toList());
    }

    public List<ScriptActionEntity> getActionData() {
        return actions.values().stream().map(ScriptActionModel::getData).collect(Collectors.toList());
    }

    public void setActions(Collection<ScriptActionEntity> actions) {
        //清空旧数据
        this.actions.clear();
        //清空选中的action
        this.checkedAction.clear();
        if (actions != null) {
            Map<Long, ScriptActionModel> map = new LinkedHashMap<>();
            int i = 0;
            for (ScriptActionEntity line : actions) {
                ScriptActionModel model = createScriptActionModel(line, i);
                map.put(model.getKey(), model);
                i++;
            }
            this.actions.putAll(map);
            //排序
            orderAction();
        }
        notifyPropertyChanged(BR.actions);
        notifyPropertyChanged(BR.checkedAction);
    }

    @NonNull
    private ScriptActionModel createScriptActionModel(ScriptActionEntity line, int i) {
        ScriptActionModel model = new ScriptActionModel();
        model.setKey(line.getId());
        //根据手势id获取
        model.setColor(getColor(line.getIndex()));
        model.setData(line);
        model.setBarEntry(new BarEntry(i, new float[]{line.getStartTime(), line.getDuration()}, line.getId()));
        return model;
    }

    public void addAction(ScriptActionEntity entity) {
        addAction(entity, true);
    }

    /**
     * 添加一个action
     *
     * @param entity      实体
     * @param correlation 关联
     */
    public void addAction(ScriptActionEntity entity, boolean correlation) {
        if (entity == null) {
            return;
        }
        //如果存在则清除id对应的数据
        ScriptActionModel model = actions.get(entity.getId());
        if (model != null) {
            actions.remove(entity.getId());
        }
        model = createScriptActionModel(entity, 0);
        //添加
        actions.put(model.getKey(), model);
        //排序
        orderAction();
        //如果需要关联,则更新当前action的后续action的开始时间(等于大于时添加对应持续时长)
        if (correlation) {
            for (Map.Entry<Long, ScriptActionModel> line : actions.entrySet()) {
                //排除自身和小于该时间的步骤
                if (line.getKey().compareTo(model.getKey()) == 0 || line.getValue().getData().getStartTime() < model.getData().getStartTime()) {
                    continue;
                }
                ScriptActionModel v = line.getValue();
                //开始时间+时长
                v.getData().setStartTime(v.getData().getStartTime() + model.getData().getDuration());
                //重新设置范围
                v.getBarEntry().setVals(new float[]{v.getData().getStartTime(), v.getData().getDuration()});
            }
        }
        //关联对应信息也变更了
        notifyPropertyChanged(BR.checkedAction);
        notifyPropertyChanged(BR.actions);
    }

    public void updateActionStartTimeByActionId(Long actionId, Long oldStartTime) {
        updateActionStartTimeByActionId(actionId, oldStartTime, true);
    }

    /**
     * 更新action时长
     *
     * @param actionId     action id
     * @param oldStartTime 旧的开始时间
     * @param correlation  是否关联
     */
    public void updateActionStartTimeByActionId(Long actionId, Long oldStartTime, boolean correlation) {
        ScriptActionModel model = actions.get(actionId);
        if (model == null || oldStartTime == null) {
            return;
        }
        //时间一样不做操作
        if (oldStartTime.compareTo(model.getData().getStartTime()) == 0) {
            return;
        }
        //获取间隔
        long d = model.getData().getStartTime() - oldStartTime;
        //更新对应model的barEntry
        model.getBarEntry().setVals(new float[]{model.getData().getStartTime(), model.getData().getDuration()});
        //关联更新后续节点的开始时间
        if (correlation) {
            boolean update = false;
            for (Map.Entry<Long, ScriptActionModel> line : actions.entrySet()) {
                if (actionId.compareTo(line.getKey()) == 0) {
                    update = true;
                    continue;
                }
                if (!update) {
                    continue;
                }
                ScriptActionModel dist = line.getValue();
                if (dist.getData().getStartTime() + d < 0) {
                    Log.e("updateActionStartTimeByActionId", "加上间隔开始时间小于0");
                    continue;
                }
                dist.getData().setStartTime(dist.getData().getStartTime() + d);
                //更新对应model的barEntry
                dist.getBarEntry().setVals(new float[]{dist.getData().getStartTime(), dist.getData().getDuration()});
            }
            //排序
            orderAction();
        }
        notifyPropertyChanged(BR.actions);
        notifyPropertyChanged(BR.checkedAction);
    }

    private void orderAction() {
        Collection<ScriptActionModel> values = actions.values();
        values = values.stream().sorted(Comparator.comparing(o -> o.getData().getStartTime())).collect(Collectors.toList());
        actions.clear();
        int i = 0;
        for (ScriptActionModel item : values) {
            item.getBarEntry().setX(i++);
            actions.put(item.getKey(), item);
        }
    }

    public void deleteAction(Long id) {
        deletePoint(id);
    }

    public void deleteAction(Long id, boolean correlation) {
        deleteAction(Collections.singleton(id), correlation);
    }

    public void deleteAction(Collection<Long> ids) {
        deletePoint(ids);
    }

    public void deleteAction(Collection<Long> ids, boolean correlation) {
        if (ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            boolean update = false;
            Long duration = 0L;
            for (Map.Entry<Long, ScriptActionModel> line : actions.entrySet()) {
                Long key = line.getKey();
                ScriptActionModel model = line.getValue();
                if (id.compareTo(key) == 0) {
                    update = true;
                    duration = model.getData().getDuration();
                    //这里还要调整统计的时间
                    root.setTotalDuration(root.getTotalDuration() - duration);
                    continue;
                }
                //后续元素排除要被删的元素进行变更开始时间,关联缩减时间
                if (update && !ids.contains(key) && correlation) {
                    //缩减开始时间
                    model.getData().setStartTime(model.getData().getStartTime() - duration);
                }
            }
            //每次运行完将这个元素删除
            ScriptActionModel remove = actions.remove(id);
            if (remove != null) {
                //同时移除point关联数据
                remove.getPointIds().forEach(k -> {
                    points.remove(k);
                    //选中
                    checkedPoint.remove(k);
                });
            }
        }
        notifyPropertyChanged(BR.checkedAction);
        notifyPropertyChanged(BR.showPoints);
        notifyPropertyChanged(BR.points);
        notifyPropertyChanged(BR.actions);
    }

    public void addPoint(Long actionId, ScriptPointEntity point) {
        addPoint(actionId, point, true);
    }

    /**
     * 添加一个point
     *
     * @param actionId    action id
     * @param point       point
     * @param correlation 是否关联
     */
    public void addPoint(Long actionId, ScriptPointEntity point, boolean correlation) {
        if (actionId == null || point == null) {
            return;
        }
        //当前point隶属于对应的action
        ScriptActionModel model = actions.get(actionId);
        if (model == null) {
            return;
        }
        //将id映射进去
        model.getPointIds().add(point.getId());
        points.put(point.getId(), point);
        //获取最后一个选中的action id
        Long activeActionId = getLastCheckActionId();
        //如果最后选中的action是当前action则需要添加一个point model进去
        if (activeActionId.compareTo(model.getKey()) == 0) {
            ScriptPointModel pointModel = createScriptPointModel(point, 0, 0L);
            showPoints.put(pointModel.getKey(), pointModel);
            //排序
            orderShowPoint();
            notifyPropertyChanged(BR.showPoints);
        }
        //追加新加入point时长
        model.getData().setDuration(model.getData().getDuration() + point.getDeltaTime());
        //根节点也不能忘记追加
        root.setTotalDuration(root.getTotalDuration() + point.getDeltaTime());
        if (!correlation) {
            return;
        }
        //关联的话就要增加后续action
        boolean update = false;
        for (Map.Entry<Long, ScriptActionModel> line : actions.entrySet()) {
            if (line.getKey().compareTo(model.getKey()) == 0) {
                update = true;
                continue;
            }
            if (!update) {
                continue;
            }
            ScriptActionModel v = line.getValue();
            //更新后续节点的开始时间
            v.getData().setStartTime(v.getData().getStartTime() + point.getDeltaTime());
        }
        notifyPropertyChanged(BR.points);
    }

    private void orderShowPoint() {
        //根据order字段进行排序
        List<ScriptPointModel> values = showPoints.values().stream()
                .sorted((o1, o2) -> Float.compare(o1.getData().getOrder(), o2.getData().getOrder()))
                .collect(Collectors.toList());
        if (values.isEmpty()) {
            return;
        }
        //获取父级action
        ScriptActionModel actionModel = actions.get(values.get(0).getActionId());
        if (actionModel == null) {
            return;
        }
        showPoints.clear();
        //action的开始时间作为基准时间
        Long startTime = actionModel.getData().getStartTime();
        for (int i = 0; i < values.size(); i++) {
            ScriptPointModel model = values.get(i);
            //更新对应bar
            model.setBarEntry(new BarEntry(i, new float[]{startTime, model.getData().getDeltaTime()}));
            //累加间隔
            startTime += model.getData().getDeltaTime();
            //重新更新颜色
            model.setColor(getColor(i));
            showPoints.put(model.getKey(), model);
        }
    }

    public void updatePointDelayTime(Long pointId, Long oldDelayTime) {
        updatePointDelayTime(pointId, oldDelayTime, true);
    }

    /**
     * 更新point间隔
     *
     * @param pointId      point id
     * @param oldDelayTime 旧间隔
     * @param correlation  是否关联
     */
    public void updatePointDelayTime(Long pointId, Long oldDelayTime, boolean correlation) {
        if (pointId == null || oldDelayTime == null) {
            return;
        }
        ScriptPointEntity point = points.get(pointId);
        if (point == null) {
            return;
        }
        if (point.getDeltaTime().compareTo(oldDelayTime) == 0) {
            return;
        }
        ScriptActionModel model = actions.get(point.getActionId());
        if (model == null) {
            return;
        }
        //d可能是负数也有可能是正数
        long d = point.getDeltaTime() - oldDelayTime;
        if (model.getData().getDuration() + d < 0) {
            Log.d("updatePointDelayTime", "间隔时间小于0!");
            return;
        }
        //action添加间隔
        model.getData().setDuration(model.getData().getDuration() + d);
        //设置间隔
        if (!correlation) {
            return;
        }
        //关联的话就需要将后面的action时间开始时间调前
        boolean update = false;
        for (Map.Entry<Long, ScriptActionModel> line : actions.entrySet()) {
            if (line.getKey().compareTo(model.getKey()) == 0) {
                update = true;
                continue;
            }
            if (!update) {
                continue;
            }
            ScriptActionModel v = line.getValue();
            if (v.getData().getStartTime() + d < 0) {
                Log.d("updatePointDelayTime", "后续action间隔时间小于0!");
                continue;
            }
            //开始时间加上间隔
            v.getData().setStartTime(v.getData().getStartTime() + d);
        }
        root.setTotalDuration(root.getTotalDuration() + d);
        notifyPropertyChanged(BR.points);
        notifyPropertyChanged(BR.actions);
    }

    public void deletePoint(Long id) {
        deletePoint(id, true);
    }

    public void deletePoint(Long id, boolean correlation) {
        deletePoint(Collections.singleton(id));
    }

    public void deletePoint(Collection<Long> ids) {
        deletePoint(ids, true);
    }

    public void deletePoint(Collection<Long> ids, boolean correlation) {
        if (ids.isEmpty()) {
            return;
        }
        Long totalDuration = 0L;
        ScriptActionModel actionModel = null;
        for (Long id : ids) {
            ScriptPointEntity point = points.remove(id);
            if (point == null) {
                continue;
            }
            showPoints.remove(id);
            totalDuration += point.getDeltaTime();
            actionModel = actions.get(point.getActionId());
        }
        if (actionModel == null) {
            return;
        }
        if (actionModel.getData().getDuration() - totalDuration < 0) {
            Log.d("deletePoint", "删除修改action开始时间小于0!");
            return;
        }
        actionModel.getData().setDuration(actionModel.getData().getDuration() - totalDuration);
        orderAction();
        root.setTotalDuration(root.getTotalDuration() - totalDuration);
        if (!correlation) {
            return;
        }
        boolean update = false;
        for (Map.Entry<Long, ScriptActionModel> line : actions.entrySet()) {
            if (line.getKey().compareTo(actionModel.getKey()) == 0) {
                update = true;
                continue;
            }
            if (!update) {
                continue;
            }
            if (line.getValue().getData().getStartTime() - totalDuration < 0) {
                Log.d("deletePoint", "删除修改后续action开始时间小于0");
                continue;
            }
            line.getValue().getData().setStartTime(line.getValue().getData().getStartTime() - totalDuration);
        }
        //添加对应action
        notifyPropertyChanged(BR.points);
        notifyPropertyChanged(BR.showPoints);
    }

    public void clearShowPointByActionId(Long actionId) {
        if (actionId == null) {
            return;
        }
        Highlight highlight = checkedAction.remove(actionId);
        if (highlight == null) {
            return;
        }
        ScriptPointModel any = showPoints.values().stream().findAny().orElse(null);
        if (any == null || any.getData().getActionId().compareTo(actionId) != 0) {
            return;
        }
        showPoints.clear();
    }

    public void addShowPointsByActionId(Long actionId, boolean force) {
        //空的话说明没有选中,应该清除数据
        if (actionId == null) {
            setShowPoints(null, null);
            return;
        }
        ScriptActionModel model = actions.get(actionId);
        if (model == null) {
            return;
        }
        if (force) {
            //这里需要删除完重新添加,把它放在最后
            Highlight highlight = checkedAction.remove(actionId);
            if (highlight == null) {
                return;
            }
            checkedAction.put(actionId, highlight);
        } else {
            //非强制更新则检查show的action
            ScriptPointModel any = showPoints.values().stream().findAny().orElse(null);
            //同一个action id就代表不需要重新添加
            if (any != null && any.getData().getActionId().compareTo(actionId) == 0) {
                return;
            }
        }
        List<ScriptPointEntity> pointEntities = new ArrayList<>();
        for (Long pointId : model.getPointIds()) {
            ScriptPointEntity pointEntity = points.get(pointId);
            if (pointEntity != null) {
                pointEntities.add(pointEntity);
            }
        }
        setShowPoints(pointEntities, actionId);
    }

    public Integer getColor(Integer i) {
        return colorsResource.isEmpty() || i == null ? Color.BLACK : colorsResource.get(i % colorsResource.size());
    }

    public boolean hasMaxTime() {
        return root != null && root.getTotalDuration() != null;
    }

    public boolean hasCount() {
        return root != null && root.getActionCount() != null;
    }

    public boolean hasName() {
        return root != null && root.getName() != null;
    }

    @Bindable
    public ObservableMap<Long, Highlight> getCheckedAction() {
        return checkedAction;
    }

    public void addCheckAction(Long id, Highlight highlight) {
        if (checkedAction.containsKey(id)) {
            checkedAction.remove(id);
        } else {
            checkedAction.put(id, highlight);
        }
        notifyPropertyChanged(BR.checkedAction);
    }

    public void addCheckPoint(Long id, Highlight h) {
        if (checkedPoint.containsKey(id)) {
            checkedPoint.remove(id);
        } else {
            checkedPoint.put(id, h);
        }
        notifyPropertyChanged(BR.checkedPoint);
    }

    public ScriptActionModel getLastAction() {
        if (!actions.isEmpty()) {
            List<ScriptActionModel> values = new ArrayList<>(actions.values());
            return values.get(values.size() - 1);
        }
        return null;
    }

    @Data
    public static class ScriptActionModel {
        private ScriptActionEntity data;
        private Long key;
        private BarEntry barEntry;
        private Integer color;
        private List<Long> pointIds = new ArrayList<>();
    }

    @Data
    public static class ScriptPointModel {
        private ScriptPointEntity data;
        private Long key;
        private BarEntry barEntry;
        private Integer color;

        public Long getActionId() {
            return data.getActionId();
        }
    }
}
