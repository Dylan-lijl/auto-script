package pub.carzy.auto_script.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;

import androidx.databinding.ViewDataBinding;

import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class BasicAction implements ScriptAction{
    protected WindowManager windowManager;
    protected MyAccessibilityService service;
    protected int screenWidth;
    protected int screenHeight;

    @Override
    public void setContext(MyAccessibilityService service) {
        this.service = service;
        windowManager = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            screenWidth = metrics.getBounds().width();
            screenHeight = metrics.getBounds().height();
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(dm);
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }
    }

    protected View.OnTouchListener createMoveListener(View view, WindowManager.LayoutParams params) {
        final int dragThreshold = 10;
        return new View.OnTouchListener() {
            private int lastX, lastY;
            private float startX, startY;
            private boolean isDragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        lastX = params.x;
                        lastY = params.y;
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;

                        if (!isDragging) {
                            if (Math.sqrt(dx * dx + dy * dy) > dragThreshold) {
                                isDragging = true; // 超过阈值才认为是拖动
                            }
                        }

                        if (isDragging) {
                            params.x = lastX + (int) dx;
                            params.y = lastY + (int) dy;
                            windowManager.updateViewLayout(view, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // 手指未移动超过阈值，触发点击事件
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        };
    }

    protected int dp2px(float dpValue) {
        float density = service.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }

    protected WindowManager.LayoutParams createBindingParams(ViewDataBinding binding) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = screenWidth - binding.getRoot().getWidth() - dp2px(50);
        params.y = screenHeight - binding.getRoot().getHeight() - dp2px(200);
        //如果设置里面有位置则设置
        Setting setting = BeanFactory.getInstance().get(Setting.class);
        if (setting != null) {
            ThreadUtil.runOnCpu(() -> {
                try {
                    FloatPoint point = setting.getPoint();
                    if (point != null) {
                        params.x = point.getX();
                        params.y = point.getY();
                    }
                } catch (Exception e) {
                    Log.w("Setting", "获取point失败:" + e.getMessage());
                }
            });
        }
        return params;
    }

    protected void removeView(ViewDataBinding binding) {
        try {
            windowManager.removeView(binding.getRoot());
        } catch (IllegalArgumentException ignored) {

        }
    }

    protected void addView(ViewDataBinding binding, WindowManager.LayoutParams params) {
        windowManager.addView(binding.getRoot(), params);
    }

    protected void reAddView(ViewDataBinding binding, WindowManager.LayoutParams params) {
        removeView(binding);
        addView(binding, params);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }
    protected void addViewTouch(View.OnTouchListener listener, View... views) {
        for (View view : views) {
            view.setOnTouchListener(listener);
        }
    }
}
