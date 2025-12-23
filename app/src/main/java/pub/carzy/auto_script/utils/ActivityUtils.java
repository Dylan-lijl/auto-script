package pub.carzy.auto_script.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.function.Consumer;


/**
 * @author admin
 */
public class ActivityUtils {
    public static <T extends TextView> void showBar(Activity activity, int viewId) {
        Consumer<String> consumer = null;
        if (activity instanceof AppCompatActivity && ((AppCompatActivity) activity).getSupportActionBar() != null) {
            consumer = title -> ((AppCompatActivity) activity).getSupportActionBar().setTitle(title);
        } else if (activity.getActionBar() != null) {
            consumer = title -> activity.getActionBar().setTitle(title);
        }

        T view = activity.findViewById(viewId);
        if (consumer != null) {
            consumer.accept(view.getText().toString());
        } else if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public static View reinstatedView(View root) {
        //复用
        // 如果已有父容器，先从父容器移除
        ViewParent parent = root.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(root);
        }
        return root;
    }

}
