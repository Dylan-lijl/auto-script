package pub.carzy.auto_script.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.databinding.ObservableInt;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import pub.carzy.auto_script.databinding.WindowDialogEditCountBinding;

/**
 * @author admin
 */
public class OverlayInputDialog {

    private final WindowManager wm;
    private final WindowDialogEditCountBinding view;
    private final int overlayFlag;

    public OverlayInputDialog(Context context, int overlayFlag) {
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        view = WindowDialogEditCountBinding.inflate(LayoutInflater.from(context));
        view.setCount(-1);
        view.countInput.setText(StringUtils.format(view.getCount()));
        view.countInput.addTextChangedListener(new DefaultTextWatch() {
            @Override
            public void afterTextChanged(Editable s) {
                String string = s.toString();
                if (s.length() == 0) {
                    view.setCount(-1);
                } else if ("-".equals(string) || "+".equals(string)) {
                    view.setCount(0);
                } else {
                    view.setCount(Integer.parseInt(string));
                }
            }
        });
        this.overlayFlag = overlayFlag;
    }

    private void processFocused(EditText text) {
        text.post(() -> {
            if (!text.isFocused()) {
                return;
            }
            text.setSelection(text.getText().length());
            InputMethodManager imm =
                    (InputMethodManager) view.getRoot().getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(text, InputMethodManager.SHOW_FORCED);
        });
    }

    public void show(int count, int tick, BiConsumer<Integer, Integer> onConfirm) {
        //设置数据
        view.setCount(count);
        view.tickInput.setText(StringUtils.format(tick));
        //成功回调
        view.btnConfirm.setOnClickListener(v -> {
            int newCount = formatNumber(view.countInput.getText().toString(), count);
            int newTick = formatNumber(view.tickInput.getText().toString(), tick);
            onConfirm.accept(newCount, newTick);
            dismiss();
        });
        //取消回调
        view.btnCancel.setOnClickListener(v -> dismiss());
        if (view.getRoot().isAttachedToWindow()) {
            return;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.CENTER;
        //添加到view
        wm.addView(ActivityUtils.reinstatedView(view.getRoot()), lp);
        processFocused(view.countInput);
        processFocused(view.tickInput);
    }

    private int formatNumber(String text, int defaultValue) {
        if (text == null || text.isEmpty()) {
            return defaultValue;
        }
        if ("-".equals(text) || "+".equals(text)) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void dismiss() {
        if (view.getRoot().isAttachedToWindow()) {
            wm.removeViewImmediate(ActivityUtils.reinstatedView(view.getRoot()));
        }
    }

    public boolean isShowing() {
        return view.getRoot().isAttachedToWindow();
    }
}

