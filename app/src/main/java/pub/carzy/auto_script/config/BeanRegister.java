package pub.carzy.auto_script.config;

import android.app.Application;
import android.content.Context;

import com.qmuiteam.qmui.skin.QMUISkinManager;

import java.util.UUID;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.impl.PrefsSetting;
import pub.carzy.auto_script.config.impl.SnowflakeGenerator;
import pub.carzy.auto_script.db.AppDatabase;
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
        if (setting.getUUID() == null) {
            setting.setUUID(UUID.randomUUID().toString().replace("-", ""));
        }
        QMUISkinManager manager = BeanFactory.getInstance().get(QMUISkinManager.class);
        manager.addSkin(1, R.style.Theme_Auto_Script);
    }

    private static void registerSetting(Application context) {
        BeanFactory register = BeanFactory.getInstance()
                .register(new PrefsSetting(context))
                .register(context)
                .register(new TypeToken<IdGenerator<Long>>() {
                }, new SnowflakeGenerator())
                .register(QMUISkinManager.defaultInstance(context))
                .register(AppDatabase.get(context));
//                .register(new ScriptAccessibilityService());
    }
}
