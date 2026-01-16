package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class DialogScriptImportModel extends BaseObservable {
    private boolean showSuccess = true;
    private boolean showFailure;
    private boolean showWarning;
    private int sizeAdapted = R.id.size_keep_unchanged;
    private int existAdapted = R.id.exist_replace;

    private List<String> success = new ArrayList<>();
    private List<String> failed = new ArrayList<>();
    private List<String> sizeWarning = new ArrayList<>();
    private List<String> existWarning = new ArrayList<>();

    @Bindable
    public boolean isHasWarning() {
        return isHasSizeWarning() || isHasExistWarning();
    }

    @Bindable
    public boolean isHasSizeWarning() {
        return !sizeWarning.isEmpty();
    }

    @Bindable
    public boolean isHasExistWarning() {
        return !existWarning.isEmpty();
    }

    @Bindable
    public boolean isHasSuccess() {
        return !success.isEmpty();
    }

    @Bindable
    public boolean isSuccessVisible() {
        return showSuccess && !success.isEmpty();
    }

    @Bindable
    public boolean isSizeWarningVisible() {
        return showWarning && sizeWarning != null && !sizeWarning.isEmpty();
    }

    @Bindable
    public boolean isExistWarningVisible() {
        return showWarning && !existWarning.isEmpty();
    }

    @Bindable
    public boolean isHasFailed() {
        return !failed.isEmpty();
    }

    @Bindable
    public boolean isFailedVisible() {
        return showFailure && !failed.isEmpty();
    }

    @Bindable
    public List<String> getSuccess() {
        return success;
    }

    public void setSuccess(List<String> success) {
        this.success = success;
        notifyPropertyChanged(BR.success);
    }

    @Bindable
    public List<String> getFailed() {
        return failed;
    }

    public void setFailed(List<String> failed) {
        this.failed = failed;
        notifyPropertyChanged(BR.failed);
    }

    @Bindable
    public int getExistAdapted() {
        return existAdapted;
    }

    public void setExistAdapted(int existAdapted) {
        this.existAdapted = existAdapted;
        notifyPropertyChanged(BR.existAdapted);
    }

    @Bindable
    public List<String> getSizeWarning() {
        return sizeWarning;
    }

    public void setSizeWarning(List<String> sizeWarning) {
        this.sizeWarning = sizeWarning;
        notifyPropertyChanged(BR.sizeWarning);
    }

    @Bindable
    public List<String> getExistWarning() {
        return existWarning;
    }

    public void setExistWarning(List<String> existWarning) {
        this.existWarning = existWarning;
        notifyPropertyChanged(BR.existWarning);
    }

    @Bindable
    public int getSizeAdapted() {
        return sizeAdapted;
    }

    public void setSizeAdapted(int sizeAdapted) {
        this.sizeAdapted = sizeAdapted;
        notifyPropertyChanged(BR.sizeAdapted);
    }

    @Bindable
    public boolean isShowSuccess() {
        return showSuccess;
    }

    public void setShowSuccess(boolean showSuccess) {
        this.showSuccess = showSuccess;
        notifyPropertyChanged(BR.showSuccess);
        notifyPropertyChanged(BR.successVisible);
    }

    @Bindable
    public boolean isShowFailure() {
        return showFailure;
    }

    public void setShowFailure(boolean showFailure) {
        this.showFailure = showFailure;
        notifyPropertyChanged(BR.showFailure);
        notifyPropertyChanged(BR.failedVisible);
    }

    @Bindable
    public boolean isShowWarning() {
        return showWarning;
    }

    public void setShowWarning(boolean showWarning) {
        this.showWarning = showWarning;
        notifyPropertyChanged(BR.showWarning);
        notifyPropertyChanged(BR.sizeWarningVisible);
        notifyPropertyChanged(BR.existWarningVisible);
    }
}
