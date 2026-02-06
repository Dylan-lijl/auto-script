package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Locale;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class DownloadProgressModel extends BaseObservable {
    private int progress = 0;
    private int max = 1;

    @Bindable
    public int getProgress() {
        return progress;
    }

    public void setProgress(int process) {
        this.progress = process;
        notifyPropertyChanged(BR.progress);
        notifyPropertyChanged(BR.progressPercentage);
    }

    @Bindable
    public String getProgressPercentage() {
        return String.format(Locale.getDefault(), "%.2f%%", progress * 100.0 / max);
    }

    @Bindable
    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        notifyPropertyChanged(BR.max);
        notifyPropertyChanged(BR.progressPercentage);
    }
}
