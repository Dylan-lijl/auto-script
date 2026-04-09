package pub.carzy.auto_script.entity;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * 蒙层配置
 *
 * @author admin
 */
public class MaskConfig extends BaseObservable implements Cloneable{
    private Integer color;
    private Integer size;
    private Boolean grid;
    private Integer lineWidth;
    private Integer gridColor;
    private Boolean scale;
    private Integer fontSize;
    private Integer fontColor;
    public MaskConfig() {
    }

    public MaskConfig(Integer color, Integer size, Boolean grid, Integer lineWidth, Integer gridColor,
                      Boolean scale, Integer fontSize, Integer fontColor) {
        this.color = color;
        this.size = size;
        this.grid = grid;
        this.lineWidth = lineWidth;
        this.gridColor = gridColor;
        this.scale = scale;
        this.fontSize = fontSize;
        this.fontColor = fontColor;
    }

    @Bindable
    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
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
    public Integer getGridColor() {
        return gridColor;
    }

    public void setGridColor(Integer gridColor) {
        this.gridColor = gridColor;
        notifyPropertyChanged(BR.gridColor);
    }

    @Bindable
    public Boolean getScale() {
        return scale;
    }

    public void setScale(Boolean scale) {
        this.scale = scale;
        notifyPropertyChanged(BR.scale);
    }

    @Bindable
    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
        notifyPropertyChanged(BR.fontSize);
    }

    @Bindable
    public Integer getFontColor() {
        return fontColor;
    }

    public void setFontColor(Integer fontColor) {
        this.fontColor = fontColor;
        notifyPropertyChanged(BR.fontColor);
    }

    @NonNull
    @Override
    public MaskConfig clone() {
        try {
            // 都是基础对象
            return (MaskConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
