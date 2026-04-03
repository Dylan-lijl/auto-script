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

    protected int id;

    public QMUIBottomSheetInputConfirmBuilder(Context context) {
        super(context);
        id = View.generateViewId();
    }


    public QMUIBottomSheetInputConfirmBuilder setConfigEditView(ConfigEditView<EditText> configEditView) {
        this.configEditView = configEditView;
        return self();
    }

    @Override
    protected void addButtonListener(View cancelButton, QMUIBottomSheet bottomSheet, View view) {
        cancelButton.setOnClickListener(e -> cancel.onClick(bottomSheet, e, view.findViewById(id)));
    }

    @Override
    protected void addSelfContentView(View view, ViewGroup viewGroup) {
        EditText editText = createEditText(context);
        if (viewGroup == null) {
            ((ViewGroup) view).addView(viewGroup = new LinearLayout(context), 0);
            viewGroup.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
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
