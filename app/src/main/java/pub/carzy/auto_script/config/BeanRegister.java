package pub.carzy.auto_script.config;

import android.content.Context;

import pub.carzy.auto_script.config.impl.PrefsSetting;
import pub.carzy.auto_script.service.ScriptAccessibilityService;

/**
 * @author admin
 */
public class BeanRegister {
    public static void run(Context context) {
        registerSetting(context);
    }

    private static void registerSetting(Context context) {
        BeanFactory.getInstance()
                .register(new PrefsSetting(context))
                .register(context)
                .register(new ScriptAccessibilityService());
    }
}
