package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.Collection;

import pub.carzy.auto_script.entity.FuturePlanEntity;

/**
 * @author admin
 */
public class AboutDevelopmentProcessModel extends BaseObservable {
    private final ObservableList<FuturePlanEntity> futurePlanData = new ObservableArrayList<>();

    public void setFuturePlanData(Collection<FuturePlanEntity> data) {
        futurePlanData.clear();
        futurePlanData.addAll(data);
    }

    @Bindable
    public ObservableList<FuturePlanEntity> getFuturePlanData() {
        return futurePlanData;
    }
}
