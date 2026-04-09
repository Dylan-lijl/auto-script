package pub.carzy.auto_script.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.utils.ActivityUtils;

/**
 * @author admin
 */
public class QMUIBottomSheetInputConfirmBuilder extends QMUIBottomSheetConfirmBuilder<QMUIBottomSheetInputConfirmBuilder> {
    protected ConfigEditView<EditText> configEditView;

    protected final int id;

    public QMUIBottomSheetInputConfirmBuilder(Context context) {
        super(context);
        id = View.generateViewId();
    }


    public QMUIBottomSheetInputConfirmBuilder setConfigEditView(ConfigEditView<EditText> configEditView) {
        this.configEditView = configEditView;
        return self();
    }

    @Override
    protected void addButtonListener(View button, QMUIBottomSheet bottomSheet, View view, OnButtonClickListener c) {
        button.setOnClickListener(e -> c.onClick(bottomSheet, e, view.findViewById(id)));
    }

    @Override
    protected void addSelfContentView(View view, ViewGroup viewGroup, View contentView) {
        super.addSelfContentView(view, viewGroup, contentView);
        viewGroup = view.findViewById(R.id.dialog_content_view);
        if (viewGroup == null) {
            return;
        }
        EditText editText = createEditText(context);
        if (configEditView != null) {
            configEditView.config(editText);
        }
        viewGroup.addView(editText);
    }

    @NonNull
    protected EditText createEditText(@NonNull Context context) {
        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        editText.setBackground(ContextCompat.getDrawable(context, R.drawable.input));
        //聚焦
        ActivityUtils.requestFocusAndShowInput(context, editText);
        // 强制设置为深灰色/黑色 (AntD 文本通常是 #333333)
        editText.setTextColor(Color.parseColor("#333333"));
        // 设置占位符颜色
        editText.setHintTextColor(Color.parseColor("#BFBFBF"));
        editText.setId(id);
        return editText;
    }
}
