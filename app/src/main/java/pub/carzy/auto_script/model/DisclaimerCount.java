package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class DisclaimerCount extends BaseObservable {
    private int tick;

    @Bindable
    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
        notifyPropertyChanged(BR.tick);
    }
}
