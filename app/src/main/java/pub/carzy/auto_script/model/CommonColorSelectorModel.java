package pub.carzy.auto_script.model;

import android.graphics.Color;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableInt;

import java.util.function.Consumer;

import lombok.Setter;
import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class CommonColorSelectorModel extends BaseObservable {
    private final ObservableInt color = new ObservableInt(0);
    @Setter
    private Consumer<Integer> colorListener;

    @Bindable
    public int getColor() {
        return color.get();
    }

    public void setColor(int color) {
        this.color.set(color);
        notifyPropertyChanged(BR.color);
        notifyPropertyChanged(BR.colorString);
        if (colorListener != null) {
            colorListener.accept(color);
        }
    }

    @Bindable
    public String getColorString() {
        // 使用这种方式可以确保输出标准的 #AARRGGBB 格式
        return String.format("#%08X", color.get());
    }

    // 对应的，在 EditText 双向绑定时，需要一个反向转换
    public void setColorString(String colorStr) {
        try {
            if (colorStr != null && (colorStr.length() == 7 || colorStr.length() == 9)) {
                int parsedColor = Color.parseColor(colorStr);
                if (this.color.get() != parsedColor) {
                    setColor(parsedColor);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
