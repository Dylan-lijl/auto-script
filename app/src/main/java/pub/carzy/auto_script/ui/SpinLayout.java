package pub.carzy.auto_script.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import pub.carzy.auto_script.R;

/**
 * 通用 SpinLayout 组件
 * 支持任意内容，旋转 spinner 和提示信息
 * @author admin
 */
public class SpinLayout extends FrameLayout {

    private View contentView;
    private View overlayView;
    private ImageView spinnerView;
    private TextView tipView;
    private ObjectAnimator spinnerAnimator;

    private boolean spinning = false;

    public SpinLayout(Context context) {
        super(context);
        init(context, null);
    }

    public SpinLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SpinLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        @DrawableRes int defaultIndicator = R.drawable.loading;
        int tipResId = 0;
        Integer tint = null;
        // 读取自定义属性
        if (attrs != null) {
            try (TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SpinLayout)) {
                defaultIndicator = ta.getResourceId(R.styleable.SpinLayout_indicator, R.drawable.loading);
                tipResId = ta.getResourceId(R.styleable.SpinLayout_tips, 0);
                if (ta.hasValue(R.styleable.SpinLayout_tint)) {
                    tint = ta.getColor(R.styleable.SpinLayout_tint, 0);
                }
            }
        }
        overlayView = new View(context);
        overlayView.setBackgroundColor(0x55FFFFFF);
        overlayView.setVisibility(GONE);
        addView(overlayView, new LayoutParams(0, 0));
        spinnerView = new ImageView(context);
        spinnerView.setImageResource(defaultIndicator);
        spinnerView.setVisibility(GONE);
        spinnerView.setMaxWidth(100);
        spinnerView.setAdjustViewBounds(true);
        if (tint != null){
            spinnerView.setColorFilter(tint);
        }
        addView(spinnerView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        tipView = new TextView(context);
        if (tipResId != 0) tipView.setText(tipResId);
        tipView.setVisibility(GONE);
        addView(tipView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        // 创建旋转动画
        spinnerAnimator = ObjectAnimator.ofFloat(spinnerView, "rotation", 0f, 360f);
        spinnerAnimator.setDuration(800);
        spinnerAnimator.setRepeatCount(ObjectAnimator.INFINITE);
    }
    @Override
    public void addView(View child) {
        captureContentView(child);
        super.addView(child);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        captureContentView(child);
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        captureContentView(child);
        super.addView(child, index, params);
    }

    private void captureContentView(View child) {
        if (contentView == null && child != overlayView && child != spinnerView && child != tipView) {
            contentView = child;
        }
    }

    public boolean isSpinning() {
        return spinning;
    }

    public void setSpinning(boolean spinning) {
        if (this.spinning == spinning) return;
        this.spinning = spinning;

        if (spinning) {
            overlayView.setVisibility(VISIBLE);
            spinnerView.setVisibility(VISIBLE);
            tipView.setVisibility(tipView.getText() != null && !tipView.getText().toString().isEmpty() ? VISIBLE : GONE);
            spinnerAnimator.start();
            if (contentView != null) contentView.setAlpha(0.5f);
            spinnerView.post(() -> spinnerView.setTranslationX(spinnerView.getWidth() / 2f));
        } else {
            overlayView.setVisibility(GONE);
            spinnerView.setVisibility(GONE);
            tipView.setVisibility(GONE);
            spinnerAnimator.cancel();
            if (contentView != null) contentView.setAlpha(1f);
        }
    }

    public void setIndicator(@DrawableRes int resId) {
        spinnerView.setImageResource(resId);
    }

    /**
     * 设置提示文字
     */
    public void setTips(String text) {
        tipView.setText(text);
    }

    /**
     * 设置提示文字（资源 ID）
     */
    public void setTips(int resId) {
        tipView.setText(resId);
    }
}
