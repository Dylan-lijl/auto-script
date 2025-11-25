package pub.carzy.auto_script.ui.adapter;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import pub.carzy.auto_script.ui.SpinLayout;

/**
 * @author admin
 */
public class SpinBindingAdapter {
    @InverseBindingAdapter(attribute = "spinning", event = "spinningAttrChanged")
    public static boolean getSpinning(SpinLayout spinLayout) {
        return spinLayout.isSpinning();
    }

    @BindingAdapter(value = {"spinning", "spinningAttrChanged"}, requireAll = false)
    public static void setSpinning(SpinLayout spinLayout, boolean spinning, InverseBindingListener listener) {
        spinLayout.setSpinning(spinning);
    }
}