package pub.carzy.auto_script.controller;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.entity.SupportLocaleResult;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class BaseController extends AbstractController {
    private Startup startup;
    private Locale locale;
    private static Map<String, Locale> LOCALE_MAP = null;

    public BaseController() {
        super();
        startup = BeanFactory.getInstance().get(Startup.class);
        init();
    }

    private void init() {
        String language = setting.getLanguage();
        if (language != null) {
            locale = Locale.forLanguageTag(language);
        }
        Map<String, Locale> localeMap = getLocaleMap(startup);
        if (localeMap == null || !localeMap.containsKey(language)) {
            return;
        }
        locale = localeMap.get(language);
    }

    public void changeLanguage(ControllerCallback<Void> callback, String language) {
        ThreadUtil.runOnCpu(() -> {
            setting.setLanguage(language);
            try {
                ThreadUtil.runOnUi(() -> callback.complete(null));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    public void getSupportLocales(ControllerCallback<SupportLocaleResult> callback) {
        ThreadUtil.runOnCpu(() -> {
            try {
                SupportLocaleResult result = new SupportLocaleResult();
                result.getLocales().putAll(getLocaleMap(startup));
                //获取当前语言
                String language = setting.getLanguage();
                if (language == null) {
                    Configuration config = new Configuration(startup.getResources().getConfiguration());
                    config.setLocale(Locale.getDefault());
                    Context localizedContext = startup.createConfigurationContext(config);
                    // 读取 language 字段显示名称
                    int resId = localizedContext.getResources().getIdentifier("language", "string", startup.getPackageName());
                    if (resId != 0) {
                        language = localizedContext.getString(resId);
                    }
                }
                if (language != null) {
                    result.setCurrentLocale(language);
                }
                ThreadUtil.runOnUi(() -> callback.complete(result));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    private static Map<String, Locale> getLocaleMap(Context context) {
        if (LOCALE_MAP == null) {
            synchronized (Object.class) {
                if (LOCALE_MAP == null) {
                    Map<String, Locale> map = new LinkedHashMap<>();
                    AssetManager assetManager = context.getAssets();
                    try {
                        String[] locales = assetManager.getLocales();
                        for (String localeStr : locales) {
                            if (localeStr.isEmpty() || "und".equals(localeStr)) {
                                continue;
                            }
                            Locale locale = Locale.forLanguageTag(localeStr);
                            // 创建对应 locale 的 Context
                            Configuration config = new Configuration(context.getResources().getConfiguration());
                            config.setLocale(locale);
                            Context localizedContext = context.createConfigurationContext(config);
                            // 读取 language 字段显示名称
                            int resId = localizedContext.getResources().getIdentifier("language", "string", context.getPackageName());
                            if (resId != 0) {
                                String name = localizedContext.getString(resId);
                                map.put(name, locale);
                            }
                        }
                        LOCALE_MAP = map;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return LOCALE_MAP;
    }

    public Locale getSyncLanguage() {
        return locale;
    }
}
