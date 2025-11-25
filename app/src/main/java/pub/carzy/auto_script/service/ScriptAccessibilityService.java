package pub.carzy.auto_script.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
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

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.databinding.FloatingButtonBinding;
import pub.carzy.auto_script.databinding.MaskViewBinding;
import pub.carzy.auto_script.model.RecordStateModel;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class ScriptAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private FloatingButtonBinding view;
    private MaskViewBinding mask;
    private RecordStateModel recordStateModel;

    private WindowManager.LayoutParams viewParams;
    private WindowManager.LayoutParams maskParams;

    @Override
    public void onCreate() {
        super.onCreate();
        //注册上去
        BeanFactory.getInstance().register(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 悬浮窗根布局
        view = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.floating_button,
                null,
                false
        );
        view.setRecordState(recordStateModel = new RecordStateModel());
        mask = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.mask_view,
                null,
                false
        );
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        viewParams = createLayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER, flag);
        maskParams = createLayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.CENTER
                , flag);
        processView();
    }

    private void processView() {
        //
        View root = mask.getRoot();
        root.setOnTouchListener((v, event) -> {
            //这里就需要记录脚本
            Log.d("x", "mask->onTouch:" + event.toString());
            return true;
        });

        View.OnTouchListener listener = createTouchListener(viewParams);
        addViewTouch(listener, view.btnFloatingPause, view.btnFloatingRecord, view.btnFloatingRun, view.btnFloatingStop);
        view.btnFloatingRecord.setOnClickListener(v -> {
            view.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            addMaskView(true);
        });
        view.btnFloatingPause.setOnClickListener(v -> {
            view.getRecordState().setState(RecordStateModel.STATE_PAUSED);
            removeMaskView();
        });
        view.btnFloatingRun.setOnClickListener(v -> {
            view.getRecordState().setState(RecordStateModel.STATE_RECORDING);
            addMaskView( true);
        });
        view.btnFloatingStop.setOnClickListener(v -> {
            view.getRecordState().setState(RecordStateModel.STATE_IDLE);
            removeMaskView();
        });
    }

    private void addViewTouch(View.OnTouchListener listener, View... views) {
        for (View view : views) {
            view.setOnTouchListener(listener);
        }
    }

    public void open() {
        addFloatingView();
    }

    private void addFloatingView() {
        removeFloatingView();
        windowManager.addView(view.getRoot(), viewParams);
    }

    private void removeMaskView() {
        try {
            windowManager.removeView(mask.getRoot());
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void removeFloatingView() {
        try {
            windowManager.removeView(view.getRoot());
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void addMaskView(boolean update) {
        windowManager.addView(mask.getRoot(), maskParams);
        if (update) {
            removeFloatingView();
            windowManager.addView(view.getRoot(), viewParams);
        }
    }

    public void close() {
        windowManager.removeView(view.getRoot());
        windowManager.removeView(mask.getRoot());
        //这里还需要释放资源等等
    }

    private static WindowManager.LayoutParams createLayoutParams(int w, int h, int gravity, Integer layoutFlag) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                w,
                h,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        // 默认居中
        params.gravity = gravity;
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

    public static void checkOpenAccessibility(ControllerCallback<Boolean> callback) {
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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // 应用被用户从最近任务划掉时调用
        disableSelf();
        stopSelf(); // 停止服务
    }

    public static void checkOpenFloatWindow(ControllerCallback<Boolean> callback) {
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
