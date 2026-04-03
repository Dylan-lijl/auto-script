package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class OperationConfig extends BaseObservable implements Cloneable {
    public OperationConfig() {
    }

    public OperationConfig(Integer size, Integer touchColor, Integer idleColor, Integer lineWidth) {
        this.size = size;
        this.touchColor = touchColor;
        this.idleColor = idleColor;
        this.lineWidth = lineWidth;
    }

    /**
     * 半径
     */
    private Integer size;
    /**
     * 触摸颜色
     */
    private Integer touchColor;
    /**
     * 空闲颜色
     */
    private Integer idleColor;
    /**
     * 线宽
     */
    private Integer lineWidth;

    @Bindable
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
        notifyPropertyChanged(BR.size);
    }

    @Bindable
    public Integer getTouchColor() {
        return touchColor;
    }

    public void setTouchColor(Integer touchColor) {
        this.touchColor = touchColor;
        notifyPropertyChanged(BR.touchColor);
    }

    @Bindable
    public Integer getIdleColor() {
        return idleColor;
    }

    public void setIdleColor(Integer idleColor) {
        this.idleColor = idleColor;
        notifyPropertyChanged(BR.idleColor);
    }

    @Bindable
    public Integer getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(Integer lineWidth) {
        this.lineWidth = lineWidth;
        notifyPropertyChanged(BR.lineWidth);
    }

    @Override
    public OperationConfig clone() {
        try {
            OperationConfig clone = (OperationConfig) super.clone();
            //基础对象
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
