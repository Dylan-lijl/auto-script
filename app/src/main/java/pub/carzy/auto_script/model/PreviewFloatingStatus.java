package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class PreviewFloatingStatus extends BaseObservable {
    public static final int NONE = 0;
    public static final int RUN = 1;
    public static final int PAUSE = 2;
    /**
     * 状态
     */
    private int status = NONE;
    /**
     * 更多状态
     */
    private boolean selected = false;
    /**
     * 模拟状态
     */
    private boolean simulate = false;

    @Bindable
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        notifyPropertyChanged(BR.status);
    }

    @Bindable
    public boolean getSimulate() {
        return simulate;
    }

    public void setSimulate(boolean simulate) {
        this.simulate = simulate;
        notifyPropertyChanged(BR.simulate);
    }

    @Bindable
    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        notifyPropertyChanged(BR.selected);
    }
}
