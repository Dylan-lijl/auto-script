package pub.carzy.auto_script.adapter;

import android.graphics.Color;

import androidx.databinding.BindingAdapter;

import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;

import pub.carzy.auto_script.model.CommonColorSelectorModel;

/**
 * @author admin
 */
public class CommonColorAdapter {
    @BindingAdapter(value = {"colorModel", "alphaBar"}, requireAll = false)
    public static void bindColorPicker(ColorPickerView view, CommonColorSelectorModel model, AlphaSlideBar alphaBar) {
        if (model == null) return;

        // 1. 关联 Alpha 条 (对应方法表中的 attachAlphaSlider)
        if (alphaBar != null) {
            view.attachAlphaSlider(alphaBar);
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

    // 处理来自 Model 的手动输入同步到 View (对应方法表中的 selectByHsvColor)
    @BindingAdapter("selectColor")
    public static void selectColor(ColorPickerView view, String colorStr) {
        try {
            int color = Color.parseColor(colorStr);
            // 防止死循环：只有当 View 当前颜色与输入不同时才改变
            if (view.getColor() != color) {
                view.selectByHsvColor(color);
            }
        } catch (Exception ignored) {
        }
    }
}
