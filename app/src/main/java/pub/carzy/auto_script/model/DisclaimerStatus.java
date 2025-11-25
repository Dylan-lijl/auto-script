package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class DisclaimerStatus extends BaseObservable {
    private boolean accepting = false;
    private boolean declining = false;

    @Bindable
    public boolean getAccepting() {
        return accepting;
    }

    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
        notifyPropertyChanged(BR.accepting);
    }

    @Bindable
    public boolean getDeclining() {
        return declining;
    }

    public void setDeclining(boolean declining) {
        this.declining = declining;
        notifyPropertyChanged(BR.declining);
    }
}
