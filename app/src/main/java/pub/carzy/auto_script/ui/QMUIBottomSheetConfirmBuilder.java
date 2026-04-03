package pub.carzy.auto_script.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetRootLayout;

import pub.carzy.auto_script.R;

/**
 * @author admin
 */
public class QMUIBottomSheetConfirmBuilder<T extends QMUIBottomSheetConfirmBuilder<T>> extends QMUIBottomSheetCustomBuilder<T> {
    protected ConfigEditView<View> configView;
    protected OnButtonClickListener cancel;
    protected OnButtonClickListener confirm;

    public QMUIBottomSheetConfirmBuilder(Context context) {
        super(context);
    }

    public T setConfigView(ConfigEditView<View> configView) {
        this.configView = configView;
        return self();
    }

    public T setCancel(OnButtonClickListener cancel) {
        this.cancel = cancel;
        return self();
    }

    public T setConfirm(OnButtonClickListener confirm) {
        this.confirm = confirm;
        return self();
    }

    @Nullable
    @Override
    protected View onCreateContentView(@NonNull QMUIBottomSheet bottomSheet, @NonNull QMUIBottomSheetRootLayout rootLayout, @NonNull Context context) {
        View view = super.onCreateContentView(bottomSheet, rootLayout, context);
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.common_confirm_dialog_view, rootLayout, false);
        }
        ViewGroup contentView = view.findViewById(R.id.dialog_content_view);
        addSelfContentView(view,contentView);
        View cancelButton = view.findViewById(R.id.cancel_button);
        if (cancelButton != null && cancel != null) {
            addButtonListener(cancelButton,bottomSheet,view);
        }
        View confirmButton = view.findViewById(R.id.confirm_button);
        if (confirmButton != null && confirm != null) {
            addButtonListener(confirmButton,bottomSheet, view);
        }
        if (configView != null) {
            configView.config(view);
        }
        return view;
    }

    protected void addButtonListener(View button, QMUIBottomSheet bottomSheet, View view) {
        button.setOnClickListener(e -> cancel.onClick(bottomSheet, e));
    }

    protected void addSelfContentView(View view, ViewGroup viewGroup) {

    }

    public interface ConfigEditView<T> {
        void config(T view);
    }

    public interface OnButtonClickListener {
        void onClick(QMUIBottomSheet sheet, View... views);
    }
}
