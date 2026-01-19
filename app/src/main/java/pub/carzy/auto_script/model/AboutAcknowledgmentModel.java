package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.entity.AcknowledgementEntity;

/**
 * @author admin
 */
public class AboutAcknowledgmentModel extends BaseObservable {
    private List<AcknowledgementEntity> data = new ArrayList<>();

    @Bindable
    public List<AcknowledgementEntity> getData() {
        return data;
    }

    public void setData(List<AcknowledgementEntity> data) {
        this.data = data;
        notifyPropertyChanged(BR.data);
    }
}
