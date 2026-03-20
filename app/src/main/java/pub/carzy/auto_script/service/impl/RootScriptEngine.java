package pub.carzy.auto_script.service.impl;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.ex.DeviceNotRootedException;
import pub.carzy.auto_script.ex.ProcessReadOrWriteIOException;
import pub.carzy.auto_script.service.AbstractScriptEngine;
import pub.carzy.auto_script.service.ScriptEngine;
import pub.carzy.auto_script.utils.Shell;

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
}
