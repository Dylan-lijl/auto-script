package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class DisclaimerModel extends BaseObservable {
    private boolean accepting = false;
    private boolean declining = false;
    private boolean view = false;
    private int tick = 10;

    @Bindable
    public boolean isAccepting() {
        return accepting;
    }

    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
        notifyPropertyChanged(BR.accepting);
        notifyPropertyChanged(BR.acceptable);
    }

    @Bindable
    public boolean isDeclining() {
        return declining;
    }

    public void setDeclining(boolean declining) {
        this.declining = declining;
        notifyPropertyChanged(BR.declining);
        notifyPropertyChanged(BR.acceptable);
    }

    @Bindable
    public boolean isView() {
        return view;
    }

    public void setView(boolean view) {
        this.view = view;
        notifyPropertyChanged(BR.view);
    }

    @Bindable
    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
        notifyPropertyChanged(BR.tick);
        notifyPropertyChanged(BR.acceptable);
    }

    @Bindable
    public boolean isAcceptable() {
        return !accepting && !declining && tick <= 0;
    }
}
