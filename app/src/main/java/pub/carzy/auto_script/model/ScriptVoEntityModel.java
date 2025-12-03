package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

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
    private ObservableList<ScriptPointEntity> points = new ObservableArrayList<>();
    private ObservableList<ScriptActionEntity> actions = new ObservableArrayList<>();
    private ObservableList<BarEntry> actionEntries = new ObservableArrayList<>();
    private Integer detailIndex = StaticValues.DEFAULT_INDEX;

    @Bindable
    public ScriptEntity getRoot() {
        return root;
    }

    public void setRoot(ScriptEntity root) {
        this.root = root;
        notifyPropertyChanged(BR.root);
    }

    @Bindable
    public ObservableList<ScriptPointEntity> getPoints() {
        return points;
    }

    public void setPoints(ObservableList<ScriptPointEntity> points) {
        this.points = points;
        notifyPropertyChanged(BR.points);
    }

    @Bindable
    public ObservableList<ScriptActionEntity> getActions() {
        return actions;
    }

    public void setActions(ObservableList<ScriptActionEntity> actions) {
        this.actions = actions;
        //重新生成对象
        List<BarEntry> list = new ArrayList<>();
        for (ScriptActionEntity action : actions) {
            list.add(new BarEntry(action.getIndex(), action.getMaxTime()));
        }
        actionEntries.addAll(list);
        notifyPropertyChanged(BR.actions);
    }

    @Bindable
    public Integer getDetailIndex() {
        return detailIndex;
    }

    public void setDetailIndex(Integer detailIndex) {
        this.detailIndex = detailIndex;
        notifyPropertyChanged(BR.detailIndex);
    }
}
