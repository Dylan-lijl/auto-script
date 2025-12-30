package pub.carzy.auto_script.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import pub.carzy.auto_script.R;


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
}
