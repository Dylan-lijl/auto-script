package pub.carzy.auto_script.service.impl;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ViewDataBinding;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class AccScriptEngine extends AbstractScriptEngine {
    protected AccessibilityService service;

    public void setAccessibilityService(AccessibilityService service) {
        this.service = service;
    }

    @Override
    public Context getContext() {
        return this.service;
    }

    @Override
    public int[] getScreenSize() {
        WindowManager manager = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = manager.getCurrentWindowMetrics();
            return new int[]{metrics.getBounds().width(), metrics.getBounds().height()};
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(dm);
            return new int[]{dm.widthPixels, dm.heightPixels};
        }
    }

    @Override
    public void init(ResultCallback callback) {
        boolean enabled = false;
        try {
            Startup context = BeanFactory.getInstance().get(Startup.class);
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                // 2. 获取当前【已启用】的所有无障碍服务列表
                List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                ComponentName myService = new ComponentName(context, MyAccessibilityService.class);
                // 3. 遍历检查你的服务是否在列表中
                for (AccessibilityServiceInfo info : enabledServices) {
                    String infoId = info.getId();
                    if (infoId == null) continue;

                    // 2. 将系统返回的 ID 转化为 ComponentName 再对比
                    ComponentName infoComponent = ComponentName.unflattenFromString(infoId);
                    if (infoComponent != null && infoComponent.equals(myService)) {
                        enabled = true;
                        break;
                    }
                }
            }
            if (enabled) {
                super.init(callback);
            } else {
                //打开无障碍弹窗
                authorizeAccessibleService(callback);
            }
        } catch (Exception e) {
            callback.onFail(ResultCallback.EXCEPTION | ResultCallback.ACCESSIBLE, e);
        }
    }

    private void authorizeAccessibleService(ResultCallback callback) {
        if (Startup.CURRENT == null || Startup.CURRENT.isDestroyed() || Startup.CURRENT.isFinishing()) {
            callback.onFail(ResultCallback.UNKNOWN);
            return;
        }
        ThreadUtil.runOnUi(() -> new AlertDialog.Builder(Startup.CURRENT)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.permission_content)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    Startup.CURRENT.startActivity(intent);
                    callback.onFail(ResultCallback.JUMP | ResultCallback.ACCESSIBLE);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    callback.onFail(ResultCallback.CANCEL | ResultCallback.ACCESSIBLE);
                })
                .show());
    }

    public static Integer getAction(Integer action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return MotionEvent.ACTION_DOWN;
            case MotionEvent.ACTION_UP:
                return MotionEvent.ACTION_UP;
            case MotionEvent.ACTION_CANCEL:
                return MotionEvent.ACTION_CANCEL;
            case MotionEvent.ACTION_OUTSIDE:
                return MotionEvent.ACTION_OUTSIDE;
            case MotionEvent.ACTION_MOVE:
                return MotionEvent.ACTION_MOVE;
            case MotionEvent.ACTION_HOVER_MOVE:
                return MotionEvent.ACTION_HOVER_MOVE;
            case MotionEvent.ACTION_SCROLL:
                return MotionEvent.ACTION_SCROLL;
            case MotionEvent.ACTION_HOVER_ENTER:
                return MotionEvent.ACTION_HOVER_ENTER;
            case MotionEvent.ACTION_HOVER_EXIT:
                return MotionEvent.ACTION_HOVER_EXIT;
            case MotionEvent.ACTION_BUTTON_PRESS:
                return MotionEvent.ACTION_BUTTON_PRESS;
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return MotionEvent.ACTION_BUTTON_RELEASE;
        }
        return action & MotionEvent.ACTION_MASK;
    }

    protected Float getEventRawY(MotionEvent event) {
        return getEventRawY(event, -1);
    }

    /**
     * 获取绝对y的坐标
     *
     * @param event 事件
     * @param index 索引
     * @return y的绝对坐标
     */
    protected Float getEventRawY(MotionEvent event, int index) {
        if (index < 0) {
            return event.getRawY();
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? event.getRawY(index) : event.getY(index) + event.getRawY() - event.getY();
    }

    protected Float getEventRawX(MotionEvent event) {
        return getEventRawX(event, -1);
    }

    /**
     * 获取绝对x的坐标
     *
     * @param event 事件
     * @param index 索引
     * @return x的绝对坐标
     */
    protected Float getEventRawX(MotionEvent event, int index) {
        if (index < 0) {
            return event.getRawX();
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? event.getRawX(index) : event.getX(index) + event.getRawX() - event.getX();
    }

    protected int getPointIndex(Integer action) {
        return action == MotionEvent.ACTION_DOWN ? 0 : ((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }

    @Override
    protected int getOverlayFlag() {
        return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
    }

    protected WindowManager.LayoutParams createBindingParams(ViewDataBinding binding) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayFlag(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        int[] screenSize = getScreenSize();
        params.x = screenSize[0] - binding.getRoot().getWidth() - QMUIDisplayHelper.dp2px(getContext(), 56);
        params.y = screenSize[1] - binding.getRoot().getHeight() - QMUIDisplayHelper.dp2px(getContext(), 56);
        //如果设置里面有位置则设置
        Setting setting = BeanFactory.getInstance().get(Setting.class);
        if (setting != null) {
            ThreadUtil.runOnCpu(() -> {
                try {
                    FloatPoint point = setting.read(SettingKey.FLOAT_POINT, null);
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
}
