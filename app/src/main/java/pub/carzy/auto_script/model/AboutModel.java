package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.entity.CheckVersionResponse;

/**
 * @author admin
 */
public class AboutModel extends BaseObservable {
    private boolean hasNew = false;
    private boolean checking = false;

    private CheckVersionResponse response;

    @Bindable
    public CheckVersionResponse getResponse() {
        return response;
    }

    public void setResponse(CheckVersionResponse response) {
        this.response = response;
        notifyPropertyChanged(BR.response);
    }

    @Bindable
    public boolean isChecking() {
        return checking;
    }

    public void setChecking(boolean checking) {
        this.checking = checking;
        notifyPropertyChanged(BR.checking);
    }

    @Bindable
    public boolean isHasNew() {
        return hasNew;
    }

    public void setHasNew(boolean hasNew) {
        this.hasNew = hasNew;
        notifyPropertyChanged(BR.hasNew);
    }
}
