package pub.carzy.auto_script.service;


import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class AbstractScriptEngine implements ScriptEngine {
    @Override
    public void start(Object... args) {

    }

    protected boolean hasFloating() {
        return Settings.canDrawOverlays(BeanFactory.getInstance().get(Startup.class));
    }

    @Override
    public void init(ResultCallback callback) {
        if (hasFloating()) {
            callback.onSuccess();
        } else {
            authorizeFloating(callback);
        }
    }

    protected void authorizeFloating(ResultCallback callback) {
        //全局不可用
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
                    callback.onFail(ResultCallback.JUMP);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    callback.onFail(ResultCallback.CANCEL);
                })
                .show());
    }
}
