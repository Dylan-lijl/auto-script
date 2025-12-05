package pub.carzy.auto_script.config;

import android.app.Application;
import android.content.Context;

import java.util.UUID;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.impl.PrefsSetting;
import pub.carzy.auto_script.config.impl.SnowflakeGenerator;
import pub.carzy.auto_script.utils.ActivityTracker;
import pub.carzy.auto_script.utils.StoreUtil;
import pub.carzy.auto_script.utils.TypeToken;

/**
 * @author admin
 */
public class BeanRegister {
    public static void run(Application context) {
        registerSetting(context);
        init();
    }

    private static void init() {
        Setting setting = BeanFactory.getInstance().get(Setting.class);
        if (setting != null) {
            if (setting.getUUID() == null) {
                setting.setUUID(UUID.randomUUID().toString().replace("-", ""));
            }
        }
    }

    private static void registerSetting(Application context) {
        BeanFactory register = BeanFactory.getInstance()
                .register(new PrefsSetting(context))
                .register(context)
                .register(new TypeToken<IdGenerator<Long>>() {
                }, new SnowflakeGenerator())
                .register(new ActivityTracker(context));
//                .register(new ScriptAccessibilityService());
    }
}
