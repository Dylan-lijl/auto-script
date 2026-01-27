package pub.carzy.auto_script.config;

import android.app.Application;

import com.qmuiteam.qmui.skin.QMUISkinManager;

import java.util.List;
import java.util.UUID;

import io.noties.markwon.Markwon;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.impl.PrefsSetting;
import pub.carzy.auto_script.config.impl.SnowflakeGenerator;
import pub.carzy.auto_script.db.AppDatabase;
import pub.carzy.auto_script.entity.Style;
import pub.carzy.auto_script.utils.ThreadUtil;
import pub.carzy.auto_script.utils.MyTypeToken;
import pub.carzy.auto_script.utils.statics.StaticValues;

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
        //获取样式
        ThreadUtil.runOnCpu(() -> {
            List<Style> styles = setting.getAllStyle();
            if (styles.isEmpty()) {
                return;
            }
            Style currentStyle = null;
            for (Style style : styles) {
                if (currentStyle == null) {
                    currentStyle = style;
                    continue;
                }
                if (currentStyle.getCurrentVersion() < style.getCurrentVersion()) {
                    currentStyle = style;
                }
            }
            if (currentStyle != null) {
                BeanFactory.getInstance().register(StaticValues.STYLE_VERSION, System.currentTimeMillis());
                BeanFactory.getInstance().register(StaticValues.STYLE_CURRENT, currentStyle);
            }
        });
    }

    private static void registerSetting(Application context) {
        BeanFactory register = BeanFactory.getInstance()
                .register(new PrefsSetting(context))
                .register(context)
                .register(new MyTypeToken<IdGenerator<Long>>() {
                }, new SnowflakeGenerator())
                .register(QMUISkinManager.defaultInstance(context))
                .register(AppDatabase.get(context));
//                .register(new ScriptAccessibilityService());
        //注册markwon解析器
        Markwon markwon = Markwon.builder(context)
                .usePlugin(ImagesPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(new Prism4j(new MyGrammarLocator()), Prism4jThemeDefault.create()))
                .build();
        register.register(markwon);
        //样式版本
        register.register(StaticValues.STYLE_VERSION, StaticValues.EMPTY_DEFAULT_LONG_VALUE);
    }
}
