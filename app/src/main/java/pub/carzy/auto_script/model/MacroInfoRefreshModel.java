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

    private Boolean delete = false;
    private Boolean deleteDetail = false;
    private Boolean save = false;

    @Bindable
    public Boolean getSave() {
        return save;
    }

    public void setSave(Boolean save) {
        this.save = save;
        notifyPropertyChanged(BR.save);
    }

    @Bindable
    public Boolean getDeleteDetail() {
        return deleteDetail;
    }

    public void setDeleteDetail(Boolean deleteDetail) {
        this.deleteDetail = deleteDetail;
        notifyPropertyChanged(BR.deleteDetail);
    }

    @Bindable
    public Boolean getDelete() {
        return delete;
    }

    public void setDelete(Boolean delete) {
        this.delete = delete;
        notifyPropertyChanged(BR.delete);
    }

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
