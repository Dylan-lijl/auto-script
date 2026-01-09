package pub.carzy.auto_script.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.service.MyAccessibilityService;
import pub.carzy.auto_script.service.impl.RecordScriptAction;


/**
 * @author admin
 */
public class ActivityUtils {
    private static Map<String, Locale> LOCALE_MAP = null;

    public static View reinstatedView(View root) {
        //复用
        // 如果已有父容器，先从父容器移除
        ViewParent parent = root.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(root);
        }
        return root;
    }

    public static void popBackStack(Fragment layout) {
        layout.requireActivity()
                .getOnBackPressedDispatcher()
                .onBackPressed();
    }

    public static QMUIDialog createDeleteMessageDialog(Context context) {
        return createDeleteMessageDialog(context, null);
    }

    public static QMUIDialog createDeleteMessageDialog(Context context, BiConsumer<QMUIDialog, Integer> confirm) {
        return createDeleteMessageDialog(context, confirm, null);
    }

    public static QMUIDialog createDeleteMessageDialog(Context context, BiConsumer<QMUIDialog, Integer> confirm, BiConsumer<QMUIDialog, Integer> cancel) {
        return new QMUIDialog.MessageDialogBuilder(context)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .addAction(R.string.cancel, (dialog, index) -> {
                    if (cancel == null) {
                        dialog.dismiss();
                    } else {
                        cancel.accept(dialog, index);
                    }
                })
                .addAction(R.string.confirm, (dialog, index) -> {
                    if (confirm == null) {
                        dialog.dismiss();
                    } else {
                        confirm.accept(dialog, index);
                    }
                })
                .create();
    }

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context, @LayoutRes int layoutId) {
        return createDeleteViewDialog(context, layoutId, null);
    }

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context, @LayoutRes int layoutId, BiConsumer<QMUIDialog, Integer> confirm) {
        return createDeleteViewDialog(context, layoutId, confirm, null);
    }

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context, @LayoutRes int layoutId, BiConsumer<QMUIDialog, Integer> confirm, BiConsumer<QMUIDialog, Integer> cancel) {
        return new QMUIDialog.CustomDialogBuilder(context)
                .setLayout(layoutId)
                .setTitle(R.string.delete_dialog_title)
                .addAction(R.string.cancel, (d, i) -> {
                    if (cancel != null) {
                        cancel.accept(d, i);
                    }
                })
                .addAction(R.string.confirm, (d, i) -> {
                    if (confirm != null) {
                        confirm.accept(d, i);
                    }
                })
                .create();
    }

    public static Map<String, Locale> getLocaleMap(Context context) {
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
                        Log.d(ActivityUtils.class.getCanonicalName(), "getLocaleMap: " + e.getMessage());
                    }
                }
            }
        }
        return LOCALE_MAP;
    }

    public static Context updateLocale(Context context, Locale locale) {
        if (locale == null) {
            return context;
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public static Locale getLocale(Context context, Setting setting) {
        String language = setting.getLanguage();
        Map<String, Locale> localeMap = ActivityUtils.getLocaleMap(context);
        // 1. 如果有 language 优先按 key 匹配
        if (language != null && localeMap != null) {
            Locale result = localeMap.get(language);
            if (result != null) {
                return result;
            }
        }
        // 2. language 为空 → 使用系统 locale
        Locale defaultLocale = Locale.getDefault();
        // 3. 遍历 map 的 value 匹配
        if (localeMap != null && !localeMap.isEmpty()) {
            for (Locale candidate : localeMap.values()) {
                if (isSameLocale(candidate, defaultLocale)) {
                    return candidate;
                }
            }
            // 4. fallback：map 不为空但没匹配 → 返回第一个
            return localeMap.values().iterator().next();
        }
        // 5. map 为空时最终 fallback → 英文
        return Locale.ENGLISH;
    }
    private static boolean isSameLocale(Locale a, Locale b) {
        if (a == null || b == null) return false;

        // 语言必须匹配
        if (!a.getLanguage().equalsIgnoreCase(b.getLanguage())) return false;

        // region 不填时视作通配匹配
        String regA = a.getCountry();
        String regB = b.getCountry();
        if (regA.isEmpty() || regB.isEmpty()) return true;

        return regA.equalsIgnoreCase(regB);
    }

    public static void checkAccessibilityServicePermission(Activity activity) {
        checkAccessibilityServicePermission(activity, null);
    }

    /**
     * 检查无障碍权限(无障碍和悬浮窗)
     *
     * @param callback 成功回调
     * @param activity activity
     */
    public static void checkAccessibilityServicePermission(Activity activity, Consumer<Boolean> callback) {
        checkOpenAccessibility((enabled) -> {
            if (!enabled) {
                //打开提示
                promptAccessibility(activity);
                return;
            }
            //检查悬浮窗权限
            checkOpenFloatWindow((e) -> {
                if (!e) {
                    promptOverlay(activity);
                    return;
                }
                if (callback != null) {
                    callback.accept(true);
                }
            });
        });
    }

    private static void promptAccessibility(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.permission_content)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    activity.startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void promptOverlay(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_prompt)
                .setMessage(R.string.float_button_permission)
                .setPositiveButton(R.string.go_to_open, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    activity.startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void checkOpenAccessibility(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
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
                final boolean tmp = enabled;
                ThreadUtil.runOnUi(() -> callback.complete(tmp));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    public static void checkOpenFloatWindow(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
            try {
                ThreadUtil.runOnUi(() -> callback.complete(Settings.canDrawOverlays(BeanFactory.getInstance().get(Startup.class))));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }
}
