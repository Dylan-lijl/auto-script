package pub.carzy.auto_script.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import pub.carzy.auto_script.R;


/**
 * @author admin
 */
public class ActivityUtils {
    public static View reinstatedView(View root) {
        //复用
        // 如果已有父容器，先从父容器移除
        ViewParent parent = root.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(root);
        }
        return root;
    }

    public static void popBackStack(Fragment layout) {
        layout.requireActivity()
                .getOnBackPressedDispatcher()
                .onBackPressed();
    }

    public static QMUIDialog createDeleteMessageDialog(Context context) {
        return createDeleteMessageDialog(context, null);
    }

    public static QMUIDialog createDeleteMessageDialog(Context context, BiConsumer<QMUIDialog, Integer> confirm) {
        return createDeleteMessageDialog(context, confirm, null);
    }

    public static QMUIDialog createDeleteMessageDialog(Context context, BiConsumer<QMUIDialog, Integer> confirm, BiConsumer<QMUIDialog, Integer> cancel) {
        return new QMUIDialog.MessageDialogBuilder(context)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .addAction(R.string.cancel, (dialog, index) -> {
                    if (cancel == null) {
                        dialog.dismiss();
                    } else {
                        cancel.accept(dialog, index);
                    }
                })
                .addAction(R.string.confirm, (dialog, index) -> {
                    if (confirm == null) {
                        dialog.dismiss();
                    } else {
                        confirm.accept(dialog, index);
                    }
                })
                .create();
    }
}
