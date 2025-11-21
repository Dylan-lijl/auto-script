package pub.carzy.auto_script.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.databinding.FloatingButtonBinding;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class ScriptAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private FloatingButtonBinding view;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 悬浮窗根布局
        view = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.floating_button,
                null,
                false
        );
        view.setRecordState(new RecordStateModel());
        //初始化参数
        WindowManager.LayoutParams params = createLayoutParams();
        View.OnTouchListener onTouchListener = createTouchListener(params);
        windowManager.addView(view.getRoot(), params);
        //给每个控件都添加监听
        view.btnFloatingRecord.setOnTouchListener(onTouchListener);
        view.btnFloatingPause.setOnTouchListener(onTouchListener);
        view.btnFloatingStop.setOnTouchListener(onTouchListener);
        view.btnFloatingRun.setOnTouchListener(onTouchListener);
        //处理监听事件
        view.btnFloatingRecord.setOnClickListener(e -> view.getRecordState().setState(RecordStateModel.STATE_RECORDING));
        view.btnFloatingPause.setOnClickListener(e -> view.getRecordState().setState(RecordStateModel.STATE_PAUSED));
        view.btnFloatingStop.setOnClickListener(e -> view.getRecordState().setState(RecordStateModel.STATE_IDLE));
        view.btnFloatingRun.setOnClickListener(e -> view.getRecordState().setState(RecordStateModel.STATE_RECORDING));
    }

    private static WindowManager.LayoutParams createLayoutParams() {
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        // 默认居中
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        return params;
    }

    private View.OnTouchListener createTouchListener(WindowManager.LayoutParams params) {
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
                            windowManager.updateViewLayout(view.getRoot(), params);
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

    public void checkOpenAccessibility(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
            boolean enabled = false;
            try {
                Startup context = BeanFactory.getInstance().get(Startup.class);
                int accessibilityEnabled = 0;
                final String service = context.getPackageName() + "/" + ScriptAccessibilityService.class.getCanonicalName();
                try {
                    accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                            Settings.Secure.ACCESSIBILITY_ENABLED);
                } catch (Settings.SettingNotFoundException e) {
                    Log.e(ScriptAccessibilityService.class.getCanonicalName(), "Error finding setting, default accessibility to not found", e);
                }

                TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
                if (accessibilityEnabled == 1) {
                    String settingValue = Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                    if (settingValue != null) {
                        colonSplitter.setString(settingValue);
                        while (colonSplitter.hasNext()) {
                            String componentName = colonSplitter.next();
                            if (componentName.equalsIgnoreCase(service)) {
                                enabled = true;
                                break;
                            }
                        }
                    }
                }
                final boolean tmp = enabled;
                ThreadUtil.runOnUi(() -> callback.complete(tmp));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    public void checkOpenFloatWindow(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
            try {
                ThreadUtil.runOnUi(() -> callback.complete(Settings.canDrawOverlays(BeanFactory.getInstance().get(Startup.class))));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }
}
