package pub.carzy.auto_script.service.impl;

import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.service.AbstractScriptEngine;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class AccScriptEngine extends AbstractScriptEngine {

    @Override
    public void init(ResultCallback callback) {
        boolean enabled = false;
        try {
            Startup context = BeanFactory.getInstance().get(Startup.class);
            int accessibilityEnabled = 0;
            final String service = context.getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();
            try {
                accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED);
            } catch (Settings.SettingNotFoundException e) {
                Log.e(RecordScriptAction.class.getCanonicalName(), "Error finding setting, default accessibility to not found", e);
                throw e;
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
            if (enabled) {
                super.init(callback);
            } else {
                //打开无障碍弹窗
                authorizeAccessibleService(callback);
            }
        } catch (Exception e) {
            callback.onFail(ResultCallback.EXCEPTION, e);
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
                    callback.onFail(ResultCallback.JUMP);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    callback.onFail(ResultCallback.CANCEL);
                })
                .show());
    }
}
