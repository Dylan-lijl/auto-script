package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class DialogScriptImportModel extends BaseObservable {
    private boolean showSuccess = true;
    private boolean showFailure;
    private boolean warning;

    @Bindable
    public boolean isShowSuccess() {
        return showSuccess;
    }

    public void setShowSuccess(boolean showSuccess) {
        this.showSuccess = showSuccess;
        notifyPropertyChanged(BR.showSuccess);
    }

    @Bindable
    public boolean isShowFailure() {
        return showFailure;
    }

    public void setShowFailure(boolean showFailure) {
        this.showFailure = showFailure;
        notifyPropertyChanged(BR.showFailure);
    }

    @Bindable
    public boolean isWarning() {
        return warning;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
        notifyPropertyChanged(BR.warning);
    }
}
