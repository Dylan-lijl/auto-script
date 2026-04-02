package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * 蒙层配置
 *
 * @author admin
 */
public class MaskConfig extends BaseObservable implements Cloneable{
    private String color;
    private Integer size;
    private Boolean grid;
    private Integer lineWidth;
    private String gridColor;

    public MaskConfig() {
    }

    public MaskConfig(String color, Integer size, Boolean grid, Integer lineWidth, String gridColor) {
        this.color = color;
        this.size = size;
        this.grid = grid;
        this.lineWidth = lineWidth;
        this.gridColor = gridColor;
    }

    @Bindable
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
        notifyPropertyChanged(BR.color);
    }

    @Bindable
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
        notifyPropertyChanged(BR.size);
    }

    @Bindable
    public Boolean getGrid() {
        return grid;
    }

    public void setGrid(Boolean grid) {
        this.grid = grid;
        notifyPropertyChanged(BR.grid);
    }

    @Bindable
    public Integer getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(Integer lineWidth) {
        this.lineWidth = lineWidth;
        notifyPropertyChanged(BR.lineWidth);
    }

    @Bindable
    public String getGridColor() {
        return gridColor;
    }

    public void setGridColor(String gridColor) {
        this.gridColor = gridColor;
        notifyPropertyChanged(BR.gridColor);
    }

    @Override
    public MaskConfig clone() {
        try {
            MaskConfig clone = (MaskConfig) super.clone();
            // 都是基础对象
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
