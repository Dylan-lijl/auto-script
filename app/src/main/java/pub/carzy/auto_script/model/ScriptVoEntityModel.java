package pub.carzy.auto_script.model;

import android.graphics.Color;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableMap;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.utils.ObservableLinkedHashMap;

/**
 * @author admin
 */
public class ScriptVoEntityModel extends BaseObservable {
    private ScriptEntity root;
    private final ObservableMap<Long, ScriptActionEntity> actions = new ObservableLinkedHashMap<>();
    private final ObservableMap<Long, BarEntry> actionBars = new ObservableLinkedHashMap<>();
    private final ObservableMap<Long, Highlight> checkedAction = new ObservableLinkedHashMap<>();
    private final ObservableMap<Long, Integer> actionColors = new ObservableLinkedHashMap<>();
    private final ObservableMap<Long, ScriptPointEntity> points = new ObservableLinkedHashMap<>();
    @Getter
    private final Map<Long, Set<Long>> pointMapByParentId = new LinkedHashMap<>();
    /**
     * 由于回调没有旧值传递,需要手动维护映射关系
     */
    private final Map<Long, Long> pointIdAndActionId = new LinkedHashMap<>();
    private final ObservableMap<Long, BarEntry> pointBars = new ObservableLinkedHashMap<>();
    private final ObservableMap<Long, Highlight> checkedPoint = new ObservableLinkedHashMap<>();
    private final ObservableMap<Long, Integer> pointColors = new ObservableLinkedHashMap<>();
    @Getter
    @Setter
    private List<Integer> colorsResource = new ArrayList<>();

    private Boolean saved = false;

    public ScriptVoEntityModel() {
        actions.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {
            @Override
            public void onMapChanged(ObservableMap<Long, ScriptActionEntity> sender, Long key) {
                //如果key不存在就删除对应数据
                ScriptActionEntity action = sender.get(key);
                if (action == null) {
                    actionBars.remove(key);
                    checkedAction.remove(key);
                    actionColors.remove(key);
                    Set<Long> remove = pointMapByParentId.remove(key);
                    if (remove != null) {
                        remove.forEach(pointIdAndActionId::remove);
                    }
                } else {
                    int index = indexOfKey(sender.keySet(), key);
                    actionBars.put(key, new BarEntry(index,
                            new float[]{action.getDownTime(), action.getUpTime() - action.getDownTime()},
                            action.getId()));
                    actionColors.put(key, colorsResource.isEmpty() ? Color.BLACK : colorsResource.get(action.getIndex() % colorsResource.size()));
                    //排序
                    if (checkedAction.containsKey(key)) {
                        Highlight remove = checkedAction.remove(key);
                        checkedAction.put(key, remove);
                    }
                }
                notifyPropertyChanged(BR.actionBars);
                notifyPropertyChanged(BR.checkedAction);
                notifyPropertyChanged(BR.actionColors);
            }
        });
        checkedAction.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {

            @Override
            public void onMapChanged(ObservableMap<Long, Highlight> sender, Long key) {
                //更新的key跟最后一个元素一样且已渲染则不更新
                if (skipUpdatePoint()) {
                    return;
                }
                Map.Entry<Long, ScriptActionEntity> entry = getLastCheckedAction();
                //清空
                pointBars.clear();
                checkedPoint.clear();
                pointColors.clear();
                //如果没有选中或ids为空则返回
                if (entry == null) {
                    return;
                }
                Set<Long> ids = pointMapByParentId.get(entry.getKey());
                ScriptActionEntity action = actions.get(entry.getKey());
                if (ids == null || ids.isEmpty() || action == null) {
                    return;
                }
                int i = 0;
                long time = action.getDownTime();
                for (Long id : ids) {
                    ScriptPointEntity point = points.get(id);
                    if (point == null) {
                        continue;
                    }
                    pointBars.put(id, new BarEntry(i, new float[]{time, point.getTime() - time}, id));
                    pointColors.put(id, colorsResource.isEmpty() ? Color.BLACK : colorsResource.get(i % colorsResource.size()));
                    time = point.getTime();
                    i++;
                }
                notifyPropertyChanged(BR.pointBars);
                notifyPropertyChanged(BR.pointColors);
                notifyPropertyChanged(BR.checkedPoint);
            }
        });
        actionBars.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {
            @Override
            public void onMapChanged(ObservableMap<Long, BarEntry> sender, Long key) {
                int i = 0;
                for (BarEntry barEntry : sender.values()) {
                    barEntry.setX(i++);
                }
            }
        });
        points.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {
            @Override
            public void onMapChanged(ObservableMap<Long, ScriptPointEntity> sender, Long key) {
                //如果key不存在就删除对应数据
                ScriptPointEntity point = sender.get(key);
                if (point == null) {
                    pointBars.remove(key);
                    checkedPoint.remove(key);
                    pointColors.remove(key);
                    Long parentId = pointIdAndActionId.remove(key);
                    if (parentId != null) {
                        Set<Long> ids = pointMapByParentId.get(parentId);
                        if (ids != null) {
                            ids.remove(key);
                        }
                        //这里需要调整action对应的barEntry
                        BarEntry barEntry = actionBars.get(parentId);
                        ScriptActionEntity action = actions.get(parentId);
                        if (barEntry != null && action != null) {
                            barEntry.setVals(new float[]{action.getDownTime(), action.getUpTime() - action.getDownTime()});
                        }
                    }
                } else {
                    Set<Long> ids = pointMapByParentId.get(point.getParentId());
                    if (ids != null) {
                        //查找index-1的点
                        long pre = -1;
                        int index = -1;
                        int i = 0;
                        for (Long id : ids) {
                            if (id.compareTo(key) == 0) {
                                index = i;
                                break;
                            }
                            pre = id;
                            i++;
                        }
                        if (index != -1) {
                            long time = -1;
                            if (pre == -1) {
                                ScriptActionEntity action = actions.get(point.getParentId());
                                if (action != null) {
                                    time = action.getDownTime();
                                }
                            } else {
                                ScriptPointEntity prePoint = points.get(pre);
                                if (prePoint != null) {
                                    time = prePoint.getTime();
                                }
                            }
                            if (time != -1) {
                                pointBars.put(key, new BarEntry(index,
                                        new float[]{point.getTime(), point.getTime() - time},
                                        point.getId()));
                                actionColors.put(key, colorsResource.isEmpty() ? Color.BLACK : colorsResource.get(index % colorsResource.size()));
                                //排序
                                if (checkedAction.containsKey(key)) {
                                    Highlight remove = checkedAction.remove(key);
                                    checkedAction.put(key, remove);
                                }
                            }
                        }
                    }
                }
                notifyPropertyChanged(BR.actionBars);
                notifyPropertyChanged(BR.checkedAction);
                notifyPropertyChanged(BR.actionColors);
            }
        });
        pointBars.addOnMapChangedCallback(new ObservableMap.OnMapChangedCallback<>() {
            @Override
            public void onMapChanged(ObservableMap<Long, BarEntry> sender, Long key) {
                int i = 0;
                for (BarEntry barEntry : sender.values()) {
                    barEntry.setX(i++);
                }
            }
        });
    }

    private boolean skipUpdatePoint() {
        Map.Entry<Long, ScriptActionEntity> entry = getLastCheckedAction();
        if (entry != null && !pointBars.isEmpty()) {
            BarEntry barEntry = pointBars.values().stream().findAny().orElse(null);
            if (barEntry != null) {
                Object data = barEntry.getData();
                if (data != null) {
                    ScriptPointEntity point = points.get((Long) data);
                    return point != null && point.getParentId().compareTo(entry.getKey()) == 0;
                }
            }
        }
        return false;
    }

    @Bindable
    public ObservableMap<Long, BarEntry> getPointBars() {
        return pointBars;
    }

    @Bindable
    public ObservableMap<Long, Highlight> getCheckedPoint() {
        return checkedPoint;
    }

    @Bindable
    public ObservableMap<Long, Integer> getPointColors() {
        return pointColors;
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
    public ObservableMap<Long, Integer> getActionColors() {
        return actionColors;
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

    public void setPoints(Map<Long, ScriptPointEntity> points) {
        this.points.clear();
        pointMapByParentId.clear();
        if (points != null) {
            this.points.putAll(points);
            points.forEach((id, point) -> {
                pointMapByParentId.computeIfAbsent(point.getParentId(), k -> new LinkedHashSet<>()).add(id);
                pointIdAndActionId.put(id, point.getParentId());
            });
        }
        notifyPropertyChanged(BR.points);
    }

    public void setPoints(Collection<ScriptPointEntity> points) {
        Map<Long, ScriptPointEntity> map = new LinkedHashMap<>();
        if (points != null) {
            points.forEach(e -> map.put(e.getId(), e));
        }
        setPoints(map);
    }

    @Bindable
    public ObservableMap<Long, ScriptActionEntity> getActions() {
        return actions;
    }

    public void setActions(Map<Long, ScriptActionEntity> actions) {
        this.actions.clear();
        this.actionBars.clear();
        if (actions != null) {
            this.actions.putAll(actions);
        }
        notifyPropertyChanged(BR.actions);
    }

    public void setActions(Collection<ScriptActionEntity> actions) {
        Map<Long, ScriptActionEntity> map = new LinkedHashMap<>();
        if (actions != null) {
            actions.forEach(e -> map.put(e.getId(), e));
        }
        setActions(map);
    }

    public boolean hasMaxTime() {
        return root != null && root.getMaxTime() != null;
    }

    public boolean hasCount() {
        return root != null && root.getCount() != null;
    }

    public boolean hasName() {
        return root != null && root.getName() != null;
    }

    @Bindable
    public ObservableMap<Long, BarEntry> getActionBars() {
        return actionBars;
    }

    @Bindable
    public ObservableMap<Long, Highlight> getCheckedAction() {
        return checkedAction;
    }

    public Map.Entry<Long, ScriptActionEntity> getLastCheckedAction() {
        if (checkedAction.isEmpty()) {
            return null;
        }
        Long key = getLastKey(checkedAction);
        if (key == null) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>(key, actions.get(key));
    }

    public Integer getLastCheckedActionIndex() {
        if (checkedAction.isEmpty()) {
            return null;
        }
        return indexOfKey(actionBars.keySet(), getLastKey(checkedAction));
    }

    public static <T> T getLastKey(Map<T, ?> map) {
        T last = null;
        for (T key : map.keySet()) {
            last = key;
        }
        return last;
    }

    public List<Long> findAfterById(Collection<Long> collection, Long id) {
        return findAfterById(collection, id, false);
    }

    public List<Long> findAfterById(Collection<Long> collection, Long id, boolean includeSelf) {
        List<Long> result = new ArrayList<>();
        boolean found = false;
        for (Long key : collection) {
            if (found) {
                result.add(key);
            }
            if (key.compareTo(id) == 0) {
                found = true;
                if (includeSelf) {
                    result.add(key);
                }
            }
        }
        return result;
    }

    public static <K> int indexOfKey(Collection<K> collection, K key) {
        int index = 0;
        for (K k : collection) {
            if ((k == null && key == null) || (k != null && k.equals(key))) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public void updateActionTime(ScriptActionEntity entity, Long d) {
        entity.setDownTime(entity.getDownTime() + d);
        entity.setMaxTime(entity.getMaxTime() + d);
        entity.setUpTime(entity.getUpTime() + d);
    }

    public void updatePointTime(ScriptPointEntity entity, Long d) {
        entity.setTime(entity.getTime() + d);
    }

    public void adjustActionTime(Long key, Long d) {
        List<Long> list = findAfterById(actions.keySet(), key);
        if (list.isEmpty()) {
            return;
        }
        long maxActionTime = -1;
        //更新后续步骤时间
        for (Long id : list) {
            ScriptActionEntity action = actions.get(id);
            if (action == null) {
                continue;
            }
            //更新后续步骤对应的点时间
            Set<Long> set = pointMapByParentId.get(action.getId());
            long max = -1;
            if (set != null) {
                for (Long item : set) {
                    ScriptPointEntity point = points.get(item);
                    if (point == null) {
                        continue;
                    }
                    updatePointTime(point, d);
                    max = Math.max(point.getTime(), max);
                }
            }
            action.setDownTime(action.getDownTime() + d);
            action.setMaxTime(max == -1 ? action.getDownTime() : max);
            action.setUpTime(action.getMaxTime());
            maxActionTime = Math.max(maxActionTime, action.getMaxTime());
        }
        //更新总结时间
        if (root != null) {
            root.setMaxTime(maxActionTime == -1 ? root.getMaxTime() : maxActionTime);
        }
    }

    public void adjustPointTime(Long key, Long d) {
        ScriptPointEntity point = points.get(key);
        if (point == null) {
            return;
        }
        Set<Long> ids = pointMapByParentId.get(point.getParentId());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> list = findAfterById(ids, key);
        long maxTime = -1L;
        for (Long id : list) {
            ScriptPointEntity item = points.get(id);
            if (item == null) {
                continue;
            }
            updatePointTime(item, d);
            maxTime = Math.max(item.getTime(), maxTime);
        }
        ScriptActionEntity action = actions.get(point.getParentId());
        if (action != null) {
            action.setMaxTime(maxTime == -1 ? action.getDownTime() : maxTime);
            action.setUpTime(action.getMaxTime());
            adjustActionTime(action.getId(), d);
        }
    }
}
