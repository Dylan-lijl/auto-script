package pub.carzy.auto_script.adapter;

import android.graphics.Color;
import android.util.Log;
import android.widget.EditText;

import androidx.databinding.BindingAdapter;

import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import pub.carzy.auto_script.model.CommonColorSelectorModel;

/**
 * @author admin
 */
public class CommonColorAdapter {
    @BindingAdapter(value = {"colorModel", "alphaBar", "brightnessBar"}, requireAll = false)
    public static void bindColorPicker(ColorPickerView view, CommonColorSelectorModel model, AlphaSlideBar alphaBar, BrightnessSlideBar brightnessBar) {
        if (model == null) return;

        // 1. 关联 Alpha 条 (对应方法表中的 attachAlphaSlider)
        if (alphaBar != null) {
            view.attachAlphaSlider(alphaBar);
        }
        if (brightnessBar != null) {
            view.attachBrightnessSlider(brightnessBar);
        }

        // 2. 初始化颜色 (对应方法表中的 setInitialColor)
        try {
            view.setInitialColor(model.getColor());
        } catch (Exception ignored) {
        }

        // 3. 设置监听：将 View 的变化同步到 Model
        view.setColorListener((ColorEnvelopeListener) (envelope, fromUser) -> {
            if (fromUser) {
                model.setColor(envelope.getColor());
            }
        });
    }

    @BindingAdapter(value = {"onFocusLost", "colorView"}, requireAll = false)
    public static void setOnFocusLost(EditText view, final CommonColorSelectorModel model, final ColorPickerView colorPickerView) {
        if (model == null) {
            return;
        }
        Runnable runnable = () -> {
            if (colorPickerView != null) {
                try {
                    colorPickerView.selectByHsvColor(model.getColor());
                } catch (IllegalAccessException e) {
                    Log.e("CommonColorAdapter", "", e);
                }
            }
        };
        // 监听焦点变化
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                model.setColorString(view.getText().toString());
                runnable.run();
            }
        });
        view.setOnEditorActionListener((v, actionId, event) -> {
            model.setColorString(view.getText().toString());
            view.clearFocus();
            runnable.run();
            return false;
        });
    }
}
