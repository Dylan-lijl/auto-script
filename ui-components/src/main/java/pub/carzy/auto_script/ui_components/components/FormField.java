package pub.carzy.auto_script.ui_components.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.databinding.BindingAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import pub.carzy.auto_script.ui_components.R;

/**
 * @author admin
 */
public class FormField extends LinearLayout {

    private final ViewGroup viewerContainer;
    private final ViewGroup editorContainer;
    private boolean edit;

    public FormField(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 加载基础外壳
        LayoutInflater.from(context).inflate(R.layout.com_form_field, this, true);
        viewerContainer = findViewById(R.id.slot_viewer_container);
        editorContainer = findViewById(R.id.slot_editor_container);

        try (TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FormField)) {
            edit = ta.getBoolean(R.styleable.FormField_edit, false);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SlotLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // 1. 收集所有声明了 slot 的子 View
        // 注意：必须先收集再移动，否则 getChildCount() 会动态改变导致循环出错
        List<View> viewers = new ArrayList<>();
        List<View> editors = new ArrayList<>();

        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            // 排除掉外壳布局本身 (通过 ID 判断)
            if (child.getId() == R.id.form_field_internal_root) continue;

            if (child.getLayoutParams() instanceof SlotLayoutParams) {
                String slotName = ((SlotLayoutParams) child.getLayoutParams()).slot;
                if ("viewer".equals(slotName)) {
                    viewers.add(child);
                } else if ("editor".equals(slotName)) {
                    editors.add(child);
                }
            }
        }

        // 2. 物理移动：从根部移除，加入到对应的 FrameLayout/LinearLayout 容器
        if (!viewers.isEmpty()) {
            viewerContainer.removeAllViews();
            Collections.reverse(viewers);
            for (View v : viewers) {
                if (v.getParent() instanceof ViewGroup) {
                    ((ViewGroup) v.getParent()).removeView(v);
                }
                viewerContainer.addView(v);
            }
        }
        if (!editors.isEmpty()) {
            editorContainer.removeAllViews();
            Collections.reverse(editors);
            for (View v : editors) {
                if (v.getParent() instanceof ViewGroup) {
                    ((ViewGroup) v.getParent()).removeView(v);
                }
                editorContainer.addView(v);
            }
        }
        requestLayout();
        applyMode();
    }

    private void applyMode() {
        if (viewerContainer != null) viewerContainer.setVisibility(edit ? GONE : VISIBLE);
        if (editorContainer != null) editorContainer.setVisibility(edit ? VISIBLE : GONE);
    }

    public void setEdit(boolean editMode) {
        this.edit = editMode;
        // 开启自动动画切换
        TransitionManager.beginDelayedTransition(this);
        applyMode();
    }
    public boolean isEdit() {
        return this.edit;
    }
    @BindingAdapter("edit")
    public static void setFormFieldEdit(FormField view, boolean isEdit) {
        if (view.isEdit() != isEdit) {
            view.setEdit(isEdit);
        }
    }

    public static class SlotLayoutParams extends LayoutParams {
        public String slot;

        public SlotLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            try (TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FormField_Child)) {
                slot = a.getString(R.styleable.FormField_Child_slot);
            }
        }
    }
}

