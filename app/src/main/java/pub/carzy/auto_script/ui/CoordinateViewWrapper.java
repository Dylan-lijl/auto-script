package pub.carzy.auto_script.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.Locale;

import lombok.Getter;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.WindowMaskViewBinding;
import pub.carzy.auto_script.utils.DefaultTextWatch;

/**
 * @author admin
 */
public class CoordinateViewWrapper {
    @Getter
    private View rootView;
    private EditText xInput;
    private EditText yInput;
    private Button btnDisplay;
    private Button btnPick;
    private View masker;
    private final WindowManager manager;
    private WindowMaskViewBinding binding;
    private int size;
    private WindowManager.LayoutParams maskParams;
    private WindowManager.LayoutParams bindingParams;
    private final int maxWidth;
    private final int maxHeight;

    public CoordinateViewWrapper(Context context, ViewGroup parent) {
        int barHeight = QMUIDisplayHelper.getStatusBarHeight(context);
        maxWidth = QMUIDisplayHelper.getScreenWidth(context);
        maxHeight = QMUIDisplayHelper.getScreenHeight(context) + barHeight;
        initView(context, parent);
        //窗口管理器
        manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initListeners();
    }

    private void initView(Context context, ViewGroup parent) {
        // 加载你提供的 XML
        rootView = LayoutInflater.from(context).inflate(R.layout.dialog_float_point, parent, false);
        // 绑定控件
        xInput = rootView.findViewById(R.id.x_input);
        yInput = rootView.findViewById(R.id.y_input);
        btnDisplay = rootView.findViewById(R.id.btn_display);
        btnPick = rootView.findViewById(R.id.btn_pick);
        binding = WindowMaskViewBinding.inflate(LayoutInflater.from(context));
        //初始化标记
        masker = new TextView(context);
        size = QMUIDisplayHelper.dp2px(context, 20);
        masker.setLayoutParams(new LinearLayout.LayoutParams(size, size));
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
        //参数
        maskParams = new WindowManager.LayoutParams(
                size, size
                , WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        maskParams.gravity = Gravity.TOP | Gravity.START;
        bindingParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        binding.bindingMasker.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // 设置一个圆角矩形，半径为宽度的一半就是圆
                // (left, top, right, bottom, radius)
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getWidth() / 2f);
            }
        });
        binding.bindingMasker.setClipToOutline(true);
    }

    private void updateMaskPosition(Integer x, Integer y) {
        if (x == null && y == null) {
            return;
        }
        if (x != null && x <= maxWidth) {
            maskParams.x = x - size / 2;
        }
        if (y != null && y <= maxHeight) {
            maskParams.y = y - size / 2;
        }
        if (masker.isAttachedToWindow()) {
            manager.updateViewLayout(masker, maskParams);
        }
    }

    @SuppressLint({"ClickableViewAccessibility"})
    private void initListeners() {
        xInput.addTextChangedListener(new DefaultTextWatch() {
            @Override
            public void afterTextChanged(Editable s) {
                String string = s.toString();
                if (!string.isBlank()) {
                    try {
                        int v = Integer.parseInt(string);
                        if (v>maxWidth){
                            v = maxWidth;
                            // 2. 防止死循环：只有当内容真的需要改变时才修改
                            String outStr = String.valueOf(v);
                            if (!string.equals(outStr)) {
                                s.replace(0, s.length(), outStr);
                                return;
                            }
                        }
                        updateMaskPosition(v, null);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        yInput.addTextChangedListener(new DefaultTextWatch() {
            @Override
            public void afterTextChanged(Editable s) {
                String string = s.toString();
                if (!string.isBlank()) {
                    try {
                        int v = Integer.parseInt(string);
                        if (v>maxHeight){
                            v = maxHeight;
                            // 2. 防止死循环：只有当内容真的需要改变时才修改
                            String outStr = String.valueOf(v);
                            if (!string.equals(outStr)) {
                                s.replace(0, s.length(), outStr);
                                return;
                            }
                        }
                        updateMaskPosition(null, v);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        btnDisplay.setOnClickListener(e -> {
            if (masker.isAttachedToWindow()) {
                manager.removeView(masker);
            } else {
                Point point = getPoint();
                updateMaskPosition(point.x, point.y);
                manager.addView(masker, maskParams);
            }
        });
        btnPick.setOnClickListener(e -> {
            if (binding.getRoot().isAttachedToWindow()) {
                manager.removeView(binding.getRoot());
            }
            binding.positionText.setText("");
            binding.bindingMasker.setVisibility(View.VISIBLE);
            Point point = getPoint();
            moveMasker(point.x, point.y);
            manager.addView(binding.getRoot(), bindingParams);
        });
        binding.backgroundRoot.setOnTouchListener((v, event) -> {
            //移动这个bindingMasker
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            // 1. 获取相对于当前全屏蒙层的坐标 (最准确)
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    binding.bindingMasker.setVisibility(View.VISIBLE);
                    // 触发移动
                    moveMasker(x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    moveMasker(x, y);
                    binding.positionText.setText(String.format(Locale.getDefault(), "X: %d , Y: %d", rawX, rawY));
                    break;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    // 抬起时，将坐标回填到之前的输入框
                    setPoint(rawX, rawY);
                    // 移除蒙层
                    safeRemove(binding.getRoot());
                    //更新位置
                    updateMaskPosition(x, y);
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
        if (x <= maxWidth) {
            xInput.setText(String.valueOf(x));
            xInput.setSelection(xInput.getText().length());
        }
        if (y <= maxHeight) {
            yInput.setText(String.valueOf(y));
            yInput.setSelection(yInput.getText().length());
        }
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                rootView.getResources().getDisplayMetrics());
    }

    public void close() {
        safeRemove(masker);
        safeRemove(binding.getRoot());
    }
}
