package pub.carzy.auto_script.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetBaseBuilder;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheetRootLayout;
import com.qmuiteam.qmui.widget.textview.QMUISpanTouchFixTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author admin
 */
public class BottomCustomSheetBuilder extends QMUIBottomSheetBaseBuilder<BottomCustomSheetBuilder> {
    private View mContentView;
    private int mLayoutResId = 0;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;
    private final List<View> mExtraViews = new ArrayList<>();
    private final List<View> buttons = new ArrayList<>();


    public void addRightButton(View button) {
        buttons.add(button);
    }

    public void removeRightButton(View button) {
        buttons.remove(button);
    }

    public BottomCustomSheetBuilder(Context context) {
        super(context);
    }

    /**
     * 直接设置一个 View
     */
    public BottomCustomSheetBuilder setContentView(@NonNull View view) {
        mContentView = view;
        return this;
    }

    /**
     * 使用 layoutRes
     */
    public BottomCustomSheetBuilder setContentView(int layoutResId) {
        mLayoutResId = layoutResId;
        return this;
    }

    /**
     * 追加子 View（常用于 Builder 内部拼装）
     */
    public BottomCustomSheetBuilder addView(@NonNull View view) {
        mExtraViews.add(view);
        return this;
    }

    public BottomCustomSheetBuilder setContentPaddingDp(int padding) {
        return setContentPaddingDp(padding, padding);
    }

    public BottomCustomSheetBuilder setContentPaddingDp(int horizontalDp, int verticalDp) {
        return setContentPaddingDp(horizontalDp, verticalDp, horizontalDp, verticalDp);
    }

    public BottomCustomSheetBuilder setContentPaddingDp(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        return this;
    }

    @Nullable
    @Override
    protected View onCreateContentView(
            @NonNull QMUIBottomSheet bottomSheet,
            @NonNull QMUIBottomSheetRootLayout rootLayout,
            @NonNull Context context) {

        View content = mContentView;

        if (content == null && mLayoutResId != 0) {
            content = LayoutInflater.from(context)
                    .inflate(mLayoutResId, rootLayout, false);
        }

        if (content == null && mExtraViews.isEmpty()) {
            return null;
        }

        // 如果只有一个 View，直接返回
        if (mExtraViews.isEmpty()) {
            return content;
        }

        // 多 View 情况，用 LinearLayout 包一层
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setPadding(
                QMUIDisplayHelper.dp2px(context, mPaddingLeft),
                QMUIDisplayHelper.dp2px(context, mPaddingTop),
                QMUIDisplayHelper.dp2px(context, mPaddingRight),
                QMUIDisplayHelper.dp2px(context, mPaddingBottom)
        );
        if (content != null) {
            removeFromParent(content);
            container.addView(content);
        }

        for (View view : mExtraViews) {
            removeFromParent(view);
            container.addView(view);
        }

        return container;
    }

    private void removeFromParent(View view) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    @Nullable
    @Override
    protected View onCreateTitleView(@NonNull QMUIBottomSheet bottomSheet,
                                     @NonNull QMUIBottomSheetRootLayout rootLayout,
                                     @NonNull Context context) {
        View view = super.onCreateTitleView(bottomSheet, rootLayout, context);
        if (view == null && buttons.isEmpty()) {
            return null;
        }
        LinearLayout rootView = new LinearLayout(context);
        rootView.setOrientation(LinearLayout.HORIZONTAL);
        rootView.setGravity(Gravity.CENTER_VERTICAL);
        rootView.setPadding(50, 0, 50, 0);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        if (view != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            view.setLayoutParams(lp);
            rootView.addView(view);
        }
        if (!buttons.isEmpty()) {
            LinearLayout btnGroup = new LinearLayout(context);
            btnGroup.setOrientation(LinearLayout.HORIZONTAL);
            btnGroup.setGravity(Gravity.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnGroup.setLayoutParams(lp);
            for (View button : buttons) {
                btnGroup.addView(button);
            }
            rootView.addView(btnGroup);
        }
        return rootView;
    }

}

