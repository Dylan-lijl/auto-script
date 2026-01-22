package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.List;
import java.util.Objects;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.entity.FAQEntity;

/**
 * @author admin
 */
public class AboutFAQModel extends BaseObservable {
    private List<FAQEntity> data;

    @Bindable
    public List<FAQEntity> getData() {
        return data;
    }

    public void setData(List<FAQEntity> data) {
        this.data = data;
        notifyPropertyChanged(BR.data);
    }
}
