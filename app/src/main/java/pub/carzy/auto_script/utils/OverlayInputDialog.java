package pub.carzy.auto_script.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.databinding.ObservableInt;

import java.util.function.Consumer;

import lombok.Getter;
import pub.carzy.auto_script.databinding.DialogEditCountBinding;

/**
 * @author admin
 */
public class OverlayInputDialog {

    private final WindowManager wm;
    private final DialogEditCountBinding view;
    @Getter
    private boolean isShowing;

    public OverlayInputDialog(Context context) {
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        view = DialogEditCountBinding.inflate(LayoutInflater.from(context));
        view.setCount(new ObservableInt(-1));
        view.getRoot().post(() -> {
            view.countInput.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) view.getRoot().getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view.countInput, InputMethodManager.SHOW_IMPLICIT);
        });

    }

    public void show(int value, Consumer<Integer> onConfirm) {
        if (isShowing) return;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.CENTER;
        view.getCount().set(value);
        view.btnConfirm.setOnClickListener(v -> {
            onConfirm.accept(view.getCount().get());
            dismiss();
        });
        view.btnCancel.setOnClickListener(v -> dismiss());
        wm.addView(ActivityUtils.reinstatedView(view.getRoot()), lp);
        isShowing = true;
    }

    public void dismiss() {
        if (isShowing) {
            wm.removeViewImmediate(ActivityUtils.reinstatedView(view.getRoot()));
            isShowing = false;
        }
    }
}

