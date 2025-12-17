package pub.carzy.auto_script.adapter;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;


import pub.carzy.auto_script.utils.Option;

/**
 * @author admin
 */
public class CodeOptionAdapter {
    @BindingAdapter("selectedValue")
    public static void setSelectedValue(Spinner spinner, Integer value) {
        if (value == null) return;

        SpinnerAdapter adapter = spinner.getAdapter();
        if (adapter == null) return;

        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item instanceof Option) {
                Object optionValue = ((Option<?>) item).getValue();
                if (value.equals(optionValue)) {
                    if (spinner.getSelectedItemPosition() != i) {
                        spinner.setSelection(i);
                    }
                    return;
                }
            }
        }
    }


    @InverseBindingAdapter(attribute = "selectedValue")
    public static Integer getSelectedValue(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        if (selected instanceof Option) {
            return ((Option<Integer>) selected).getValue();
        }
        return null;
    }

    @BindingAdapter("selectedValueAttrChanged")
    public static void setListener(
            Spinner spinner,
            InverseBindingListener listener) {

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (listener != null) {
                            listener.onChange();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
    }

}
