package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class MacroInfoRefreshModel extends BaseObservable {
    private Boolean info = false;
    private Boolean detail = false;

    @Bindable
    public Boolean getInfo() {
        return info;
    }

    public void setInfo(Boolean info) {
        this.info = info;
        notifyPropertyChanged(BR.info);
    }

    @Bindable
    public Boolean getDetail() {
        return detail;
    }

    public void setDetail(Boolean detail) {
        this.detail = detail;
        notifyPropertyChanged(BR.detail);
    }
}
