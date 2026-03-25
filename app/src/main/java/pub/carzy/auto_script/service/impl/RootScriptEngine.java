package pub.carzy.auto_script.service.impl;

import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.databinding.ViewDataBinding;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.ex.DeviceNotRootedException;
import pub.carzy.auto_script.ex.ProcessReadOrWriteIOException;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class RootScriptEngine extends AbstractScriptEngine {
    protected Process cmdProcess;

    @Override
    public void init(ResultCallback callback) {
        try {
            cmdProcess = Shell.getRootProcess();
            doSubInit(callback);
            super.init(callback);
        } catch (DeviceNotRootedException | ProcessReadOrWriteIOException e) {
            callback.onFail(ResultCallback.EXCEPTION | ResultCallback.ROOT, e);
        }
    }

    protected void doSubInit(ResultCallback callback) {

    }

    @Override
    protected void authorizeFloating(ResultCallback callback) {
        //发送adb命令静默授权
        Shell.grantOverlayPermissionSilently(cmdProcess, BeanFactory.getInstance().get(Startup.class).getPackageName());
        if (hasFloating()) {
            callback.onSuccess();
        } else {
            //回退到弹窗
            super.authorizeFloating(callback);
        }
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

    @Override
    public void close() {
        super.close();
        if (cmdProcess != null) {
            cmdProcess.destroy();
        }
    }
}
