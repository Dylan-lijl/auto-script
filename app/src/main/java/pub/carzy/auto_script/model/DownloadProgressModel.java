package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class DownloadProgressModel extends BaseObservable {
    private int progress = 0;

    @Bindable
    public int getProgress() {
        return progress;
    }

    public void setProgress(int process) {
        this.progress = process;
        notifyPropertyChanged(BR.progress);
    }
}
