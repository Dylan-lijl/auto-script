package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import lombok.Data;
import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class FloatPoint extends BaseObservable {
    private int x;
    private int y;

    public FloatPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public FloatPoint() {
    }

    @Bindable
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        notifyPropertyChanged(BR.x);
    }

    @Bindable
    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        notifyPropertyChanged(BR.y);
    }
}
