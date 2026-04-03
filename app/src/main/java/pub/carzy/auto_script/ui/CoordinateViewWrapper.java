package pub.carzy.auto_script.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.Locale;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.WindowMaskViewBinding;

/**
 * @author admin
 */
public class CoordinateViewWrapper {
    private final View rootView;
    private final EditText xInput;
    private final EditText yInput;
    private final Button btnDisplay;
    private final Button btnPick;
    private View masker;
    private View bindingMasker;
    private final WindowManager manager;
    private final WindowMaskViewBinding binding;

    public CoordinateViewWrapper(Context context, ViewGroup parent) {
        // 加载你提供的 XML
        rootView = LayoutInflater.from(context).inflate(R.layout.dialog_float_point, parent, false);
        // 绑定控件
        xInput = rootView.findViewById(R.id.x_input);
        yInput = rootView.findViewById(R.id.y_input);
        btnDisplay = rootView.findViewById(R.id.btn_display);
        btnPick = rootView.findViewById(R.id.btn_pick);
        createMask(context);
        //窗口管理器
        manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        binding = WindowMaskViewBinding.inflate(LayoutInflater.from(context));
        initDefaultStyles();
        initListeners();
    }

    private void createMask(Context context) {
        //初始化标记
        masker = new ImageView(context);
        masker.setLayoutParams(new LinearLayout.LayoutParams(QMUIDisplayHelper.dp2px(context, 20), QMUIDisplayHelper.dp2px(context, 20)));
        masker.setBackgroundColor(context.getColor(R.color.link));
        //设置成圆
        // 1. 定义裁纸刀（OutlineProvider）
        masker.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // 设置一个圆角矩形，半径为宽度的一半就是圆
                // (left, top, right, bottom, radius)
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getWidth() / 2f);
            }
        });
        // 2. 开启裁剪开关（非常重要，不设置这一行没效果）
        masker.setClipToOutline(true);
        //初始化标记
        bindingMasker = new ImageView(context);
        bindingMasker.setLayoutParams(new LinearLayout.LayoutParams(QMUIDisplayHelper.dp2px(context, 20), QMUIDisplayHelper.dp2px(context, 20)));
        bindingMasker.setBackgroundColor(context.getColor(R.color.link));
        //设置成圆
        // 1. 定义裁纸刀（OutlineProvider）
        bindingMasker.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // 设置一个圆角矩形，半径为宽度的一半就是圆
                // (left, top, right, bottom, radius)
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getWidth() / 2f);
            }
        });
        // 2. 开启裁剪开关（非常重要，不设置这一行没效果）
        bindingMasker.setClipToOutline(true);
    }

    @SuppressLint({"ClickableViewAccessibility"})
    private void initListeners() {
        btnDisplay.setOnClickListener(e -> {
            if (masker.isAttachedToWindow()) {
                manager.removeView(masker);
            } else {
                manager.addView(masker, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
        btnPick.setOnClickListener(e -> {
            if (binding.getRoot().isAttachedToWindow()) {
                manager.removeView(binding.getRoot());
            }
            binding.positionText.setText("");
            binding.bindingMasker.setVisibility(View.VISIBLE);
            manager.addView(binding.getRoot(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        });
        binding.backgroundRoot.setOnTouchListener((v, event) -> {
            //移动这个bindingMasker
            float rawX = event.getRawX();
            float rawY = event.getRawY();
            // 1. 获取相对于当前全屏蒙层的坐标 (最准确)
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    binding.bindingMasker.setVisibility(View.VISIBLE);
                    // 触发移动
                    moveMasker(x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    moveMasker(x, y);
                    binding.positionText.setText(String.format(Locale.getDefault(),"X: %.0f , Y: %.0f", rawX, rawY));
                    break;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    // 抬起时，将坐标回填到之前的输入框
                    xInput.setText(String.valueOf((int) rawX));
                    yInput.setText(String.valueOf((int) rawY));
                    // 移除蒙层
                    safeRemove(binding.getRoot());
                    //更新位置
                    if (masker.isAttachedToWindow()){
                        //todo 坐标
                        manager.updateViewLayout(masker,new WindowManager.LayoutParams());
                    }
                    break;
            }
            return true; // 必须为 true
        });
    }

    private void moveMasker(float x, float y) {
        // 减去圆点宽度的一半，使圆心对准手指
        binding.bindingMasker.setTranslationX(x - (binding.bindingMasker.getWidth() / 2f));
        binding.bindingMasker.setTranslationY(y - (binding.bindingMasker.getHeight() / 2f));
    }

    private void safeRemove(View view) {
        if (view != null && view.isAttachedToWindow()) {
            manager.removeView(view);
        }
    }

    private void initDefaultStyles() {
        // 可以在这里统一处理你之前提到的 AntD Padding 逻辑
        int ph = dp2px(12);
        int pv = dp2px(8);
        xInput.setPadding(ph, pv, ph, pv);
        yInput.setPadding(ph, pv, ph, pv);
    }

    // 获取当前输入的坐标
    public Point getPoint() {
        try {
            int x = Integer.parseInt(xInput.getText().toString());
            int y = Integer.parseInt(yInput.getText().toString());
            return new Point(x, y);
        } catch (NumberFormatException e) {
            return new Point(0, 0);
        }
    }

    // 设置坐标
    public void setPoint(int x, int y) {
        xInput.setText(String.valueOf(x));
        yInput.setText(String.valueOf(y));
        // 顺便把光标移到末尾
        xInput.setSelection(xInput.getText().length());
        yInput.setSelection(yInput.getText().length());
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                rootView.getResources().getDisplayMetrics());
    }
}
