package pub.carzy.auto_script.config;

import android.content.Context;

import java.util.UUID;

import pub.carzy.auto_script.config.impl.PrefsSetting;
import pub.carzy.auto_script.config.impl.SnowflakeGenerator;
import pub.carzy.auto_script.utils.TypeToken;

/**
 * @author admin
 */
public class BeanRegister {
    public static void run(Context context) {
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

    private static void registerSetting(Context context) {
        BeanFactory.getInstance()
                .register(new PrefsSetting(context))
                .register(context)
                .register(new TypeToken<IdGenerator<Long>>() {
                }, new SnowflakeGenerator());
//                .register(new ScriptAccessibilityService());
    }
}
