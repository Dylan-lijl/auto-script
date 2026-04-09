package pub.carzy.auto_script.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetBaseBuilder;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetRootLayout;

import lombok.Data;

/**
 * 自定义
 *
 * @author admin
 */
public class QMUIBottomSheetCustomBuilder<T extends QMUIBottomSheetCustomBuilder<T>>
        extends QMUIBottomSheetBaseBuilder<T> {
    protected final Context context;
    protected ViewWrapper wrapper;

    public QMUIBottomSheetCustomBuilder(Context context) {
        super(context);
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T setContentView(@LayoutRes int layoutId) {
        wrapper = new ViewWrapper(layoutId);
        return self();
    }

    public T setContentView(View view) {
        wrapper = new ViewWrapper(view);
        return self();
    }

    @Nullable
    @Override
    protected View onCreateContentView(@NonNull QMUIBottomSheet bottomSheet, @NonNull QMUIBottomSheetRootLayout rootLayout, @NonNull Context context) {
        if (wrapper != null) {
            return wrapper.get(context, rootLayout,false);
        }
        return null;
    }

    @Data
    protected static class ViewWrapper {
        private View view;
        private int resId = -1;

        public ViewWrapper(View view) {
            this.view = view;
        }

        public ViewWrapper(@LayoutRes int resId) {
            this.resId = resId;
        }

        public View get(Context context, ViewGroup group,boolean attachToRoot) {
            if (view != null) {
                return view;
            }
            if (resId != -1) {
                return LayoutInflater.from(context).inflate(resId, group,attachToRoot);
            }
            return null;
        }
    }
}
