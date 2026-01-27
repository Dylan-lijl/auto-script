package pub.carzy.auto_script.model;

import android.graphics.Color;
import android.util.Log;

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
            if (colorStr.startsWith("#")){
                int length = colorStr.length();
                // 处理简写格式
                if (length == 4) {
                    // #RGB -> #RRGGBB
                    colorStr = "#" + repeat(colorStr.charAt(1)) + repeat(colorStr.charAt(2)) + repeat(colorStr.charAt(3));
                    length = colorStr.length();
                } else if (length == 5) {
                    // #ARGB -> #AARRGGBB
                    colorStr = "#" + repeat(colorStr.charAt(1)) + repeat(colorStr.charAt(2)) + repeat(colorStr.charAt(3)) + repeat(colorStr.charAt(4));
                    length = colorStr.length();
                }
                if (length == 7 || length == 9) {
                    int parsedColor = Color.parseColor(colorStr);
                    if (this.color.get() != parsedColor) {
                        setColor(parsedColor);
                    }
                }
            }else{
                setColor(Color.WHITE);
            }
        } catch (Exception e) {
            Log.e("CommonColorSelectorModel", "setColorString", e);
        }
    }
    private String repeat(char c) {
        return String.valueOf(c) + c;
    }
}
