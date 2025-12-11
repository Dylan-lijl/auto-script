package pub.carzy.auto_script.model;

import android.graphics.Color;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableArrayMap;
import androidx.databinding.ObservableList;
import androidx.databinding.ObservableMap;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Pair;
import lombok.Getter;
import lombok.Setter;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;
import pub.carzy.auto_script.utils.statics.StaticValues;

/**
 * @author admin
 */
public class ScriptVoEntityModel extends BaseObservable {
    private ScriptEntity root;
    private final ObservableArrayMap<Long, ScriptPointEntity> points = new ObservableArrayMap<>();
    private final ObservableArrayMap<Long, ScriptActionEntity> actions = new ObservableArrayMap<>();
    private final ObservableArrayMap<Long, BarEntry> actionBars = new ObservableArrayMap<>();
    private final ObservableList<Long> checkedAction = new ObservableArrayList<>();
    private final ObservableMap<Long, Integer> actionColors = new ObservableArrayMap<>();
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
                    notifyPropertyChanged(BR.checkedAction);
                } else {
                    int index = ((ObservableArrayMap<Long, ScriptActionEntity>) sender)
                            .indexOfKey(key);
                    actionBars.put(key, new BarEntry(index,
                            new float[]{action.getDownTime(), action.getUpTime() - action.getDownTime()},
                            action.getId()));
                    actionColors.put(key, colorsResource.isEmpty() ? Color.BLACK : colorsResource.get(action.getIndex() % colorsResource.size()));
                    //排序
                    if (checkedAction.contains(key)) {
                        checkedAction.remove(key);
                        checkedAction.add(key);
                    }
                }
                notifyPropertyChanged(BR.actionBars);
                notifyPropertyChanged(BR.checkedAction);
                notifyPropertyChanged(BR.actionColors);
            }
        });
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
        if (points != null) {
            this.points.putAll(points);
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
        ObservableMap<Long, ScriptActionEntity> map = new ObservableArrayMap<>();
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
    public ObservableArrayMap<Long, BarEntry> getActionBars() {
        return actionBars;
    }

    @Bindable
    public ObservableList<Long> getCheckedAction() {
        return checkedAction;
    }

    public List<Long> findAfterById(Long id) {
        int index = actions.indexOfKey(id);
        if (index < 0) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(actions.keySet()).subList(index + 1, actions.size());
        }
    }
}
