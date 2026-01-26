package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.entity.TroubleshootingEntity;

/**
 * @author admin
 */
public class AboutTroubleshootingModel extends BaseObservable {
    private List<TroubleshootingEntity> data = new ArrayList<>();

    @Bindable
    public List<TroubleshootingEntity> getData() {
        return data;
    }

    public void setData(List<TroubleshootingEntity> data) {
        this.data = data;
        notifyPropertyChanged(BR.data);
    }
}
