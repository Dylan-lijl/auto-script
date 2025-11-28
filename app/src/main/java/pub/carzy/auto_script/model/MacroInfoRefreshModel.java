package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class MacroInfoRefreshModel extends BaseObservable {
    private Boolean info = false;

    @Bindable
    public Boolean getInfo() {
        return info;
    }

    public void setInfo(Boolean info) {
        this.info = info;
        notifyPropertyChanged(BR.info);
    }
}
