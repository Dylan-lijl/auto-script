package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableList;

import lombok.Data;
import lombok.Getter;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;

/**
 * @author admin
 */
public class ScriptVoEntityModel extends BaseObservable {
    private ScriptEntity root;
    private ObservableList<ScriptPointEntity> points = new ObservableArrayList<>();
    private ObservableList<ScriptActionEntity> actions = new ObservableArrayList<>();

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
        notifyPropertyChanged(BR.actions);
    }
}
