package pub.carzy.auto_script.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseMethod;

/**
 * @author admin
 */
public class Converter {
    @InverseMethod("stringToLong")
    public static String longToString(long value) {
        return String.valueOf(value);
    }

    // 将 EditText 的 String 转回 Long 存入 data.downTime
    public static long stringToLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    @InverseMethod("stringToInt")
    public static String intToString(int value) {
        return String.valueOf(value);
    }

    public static int stringToInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    @InverseMethod("stringToFloat")
    public static String floatToString(float value) {
        return String.valueOf(value);
    }

    public static float stringToFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    @BindingAdapter("minVal")
    public static void setMinVal(EditText view, int min) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.isEmpty()) return;

                try {
                    long val = Long.parseLong(str);
                    if (val < min) {
                        view.setText(String.valueOf(min));
                    }
                    view.setSelection(view.getText().length());
                } catch (NumberFormatException ignored) {
                }
            }
        });
    }
}
