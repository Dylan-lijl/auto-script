package pub.carzy.auto_script.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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
        View contentView = super.onCreateContentView(bottomSheet, rootLayout, context);
        View view = LayoutInflater.from(context).inflate(R.layout.common_confirm_dialog_view, rootLayout, false);
        ViewGroup viewGroup = view.findViewById(R.id.dialog_content_view);
        addSelfContentView(view, viewGroup, contentView);
        View cancelButton = view.findViewById(R.id.cancel_button);
        if (cancelButton != null && cancel != null) {
            addButtonListener(cancelButton, bottomSheet, view,cancel);
        }
        View confirmButton = view.findViewById(R.id.confirm_button);
        if (confirmButton != null && confirm != null) {
            addButtonListener(confirmButton, bottomSheet, view,confirm);
        }
        if (configView != null) {
            configView.config(view);
        }
        return view;
    }

    protected void addButtonListener(View button, QMUIBottomSheet bottomSheet, View view, OnButtonClickListener c) {
        button.setOnClickListener(e -> c.onClick(bottomSheet, e));
    }

    protected void addSelfContentView(View view, ViewGroup viewGroup, View contentView) {
        if (viewGroup == null) {
            ((ViewGroup) view).addView(viewGroup = new LinearLayout(context));
            viewGroup.setId(R.id.dialog_content_view);
            viewGroup.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (contentView != null) {
            viewGroup.addView(contentView, 0);
        }
    }

    public interface ConfigEditView<T> {
        void config(T view);
    }

    public interface OnButtonClickListener {
        void onClick(QMUIBottomSheet sheet, View... views);
    }
}
