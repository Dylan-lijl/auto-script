package pub.carzy.auto_script.utils;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUIWrapContentListView;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogView;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;
import com.qmuiteam.qmui.widget.popup.QMUIQuickAction;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.entity.SettingProxy;
import pub.carzy.auto_script.ex.DeviceNotRootedException;
import pub.carzy.auto_script.ex.UnauthorizedRootAccessException;
import pub.carzy.auto_script.core.ScriptEngine;


/**
 * @author admin
 */
public class ActivityUtils {
    public static final String version1 = "v1.0.1";
    public static final String version2 = "v1.1.0";
    private static volatile Map<String, Locale> LOCALE_MAP = null;

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

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context) {
        return createDeleteViewDialog(context, null);
    }

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context, @LayoutRes Integer layoutId) {
        return createDeleteViewDialog(context, layoutId, null);
    }

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context, @LayoutRes Integer layoutId, BiConsumer<QMUIDialog, Integer> confirm) {
        return createDeleteViewDialog(context, layoutId, confirm, null);
    }

    public static QMUIDialog createDeleteViewDialog(@NonNull Context context, @LayoutRes Integer layoutId, BiConsumer<QMUIDialog, Integer> confirm, BiConsumer<QMUIDialog, Integer> cancel) {
        QMUIDialog.CustomDialogBuilder builder = new QMUIDialog.CustomDialogBuilder(context) {
            @Nullable
            @Override
            protected View onCreateContent(QMUIDialog dialog, QMUIDialogView parent, Context context) {
                return layoutId == null ? new TextView(context) : super.onCreateContent(dialog, parent, context);
            }
        };
        return builder
                .setTitle(R.string.delete_dialog_title)
                .addAction(R.string.cancel, (d, i) -> {
                    if (cancel != null) {
                        cancel.accept(d, i);
                    } else {
                        d.dismiss();
                    }
                })
                .addAction(R.string.confirm, (d, i) -> {
                    if (confirm != null) {
                        confirm.accept(d, i);
                    } else {
                        d.dismiss();
                    }
                })
                .create();
    }

    /**
     * 获取本地化映射map,从资源文件夹查找
     *
     * @param context 上下文
     * @return map
     */
    public static Map<String, Locale> getLocaleMap(Context context) {
        if (LOCALE_MAP == null) {
            synchronized (Object.class) {
                if (LOCALE_MAP == null) {
                    Map<String, Locale> map = new LinkedHashMap<>();
                    //从上下文获取资源管理器
                    AssetManager assetManager = context.getAssets();
                    try {
                        //获取可使用的本地化
                        String[] locales = assetManager.getLocales();
                        for (String localeStr : locales) {
                            if (localeStr.isEmpty() || "und".equals(localeStr)) {
                                continue;
                            }
                            //根据本地化字符串加载对应本地化对象
                            Locale locale = Locale.forLanguageTag(localeStr);
                            // 创建对应 locale 的 Context
                            Configuration config = new Configuration(context.getResources().getConfiguration());
                            config.setLocale(locale);
                            Context localizedContext = context.createConfigurationContext(config);
                            //读取本地化language字段-->strings.xml-->language
                            @SuppressLint("DiscouragedApi") int resId = localizedContext.getResources().getIdentifier("language", "string", context.getPackageName());
                            if (resId != 0) {
                                String name = localizedContext.getString(resId);
                                //放入map
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

    /**
     * 切换本地化
     *
     * @param context c
     * @param locale  l
     * @return 新的上下文
     */
    public static Context updateLocale(Context context, Locale locale) {
        if (locale == null) {
            return context;
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    /**
     * 获取本地化
     * 如果用户有存储则使用,没有则默认英语
     *
     * @param context 上下文
     * @param setting 配置类
     * @return 本地化
     */
    public static Locale getLocale(Context context, Setting setting) {
        //从配置中获取
        String language = setting.read(SettingKey.LANGUAGE, null);
        //
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

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "null";
        }
    }

    public static Integer currentApiVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    public static final Map<Integer, String> VERSION_API_MAP = new LinkedHashMap<>();
    public static final String API_PREFIX = "Android";

    static {
        VERSION_API_MAP.put(36, API_PREFIX + " 16");
        VERSION_API_MAP.put(35, API_PREFIX + " 15");
        VERSION_API_MAP.put(34, API_PREFIX + " 14");
        VERSION_API_MAP.put(33, API_PREFIX + " 13");
        VERSION_API_MAP.put(32, API_PREFIX + " 12L");
        VERSION_API_MAP.put(31, API_PREFIX + " 12");
        VERSION_API_MAP.put(30, API_PREFIX + " 11");
        VERSION_API_MAP.put(29, API_PREFIX + " 10");
        VERSION_API_MAP.put(28, API_PREFIX + " 9");
        VERSION_API_MAP.put(27, API_PREFIX + " 8.1");
        VERSION_API_MAP.put(26, API_PREFIX + " 8.0");
        VERSION_API_MAP.put(25, API_PREFIX + " 7.1");
        VERSION_API_MAP.put(24, API_PREFIX + " 7.0");
        VERSION_API_MAP.put(23, API_PREFIX + " 6.0");
        VERSION_API_MAP.put(22, API_PREFIX + " 5.1");
        VERSION_API_MAP.put(21, API_PREFIX + " 5.0");
        VERSION_API_MAP.put(19, API_PREFIX + " 4.4");
        VERSION_API_MAP.put(18, API_PREFIX + " 4.3");
        VERSION_API_MAP.put(17, API_PREFIX + " 4.2");
        VERSION_API_MAP.put(16, API_PREFIX + " 4.1");
        VERSION_API_MAP.put(15, API_PREFIX + " 4.0.3");
        VERSION_API_MAP.put(14, API_PREFIX + " 4.0");
    }

    public static Map<Integer, String> getApiMap() {
        return VERSION_API_MAP;
    }

    public static String getApiName(Integer version) {
        return getApiMap().get(version);
    }

    public static Integer minSdk(Context context) {
        return context.getApplicationInfo().minSdkVersion;
    }

    public static Integer targetSdk(Context context) {
        return context.getApplicationInfo().targetSdkVersion;
    }

    public static QMUIQuickAction createQuickAction(Context context) {
        return QMUIPopups.quickAction(context,
                        QMUIDisplayHelper.dp2px(context, 56),
                        QMUIDisplayHelper.dp2px(context, 56))
                .shadow(true)
                .skinManager(QMUISkinManager.defaultInstance(context))
                .edgeProtection(QMUIDisplayHelper.dp2px(context, 20));
    }

    public static Drawable getDrawable(Context context, int drawableId, int colorId) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
        if (drawable == null) {
            return null;
        }
        int color = ContextCompat.getColor(context, colorId);
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    public static void openToBrowser(Context context, String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public static void copyToClipboard(Context context, String label, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    public static void setOnBackPressed(AppCompatActivity activity, Consumer<Boolean> callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
            // 使用数组或 AtomicReference 来绕过 lambda 内部对自身的引用限制
            OnBackInvokedCallback[] callbackWrapper = new OnBackInvokedCallback[1];
            callbackWrapper[0] = () -> {
                // 触发时自动注销，防止二次分发死循环
                if (callbackWrapper[0] != null) {
                    dispatcher.unregisterOnBackInvokedCallback(callbackWrapper[0]);
                    callbackWrapper[0] = null;
                }
                callback.accept(true);
            };
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callbackWrapper[0]);
            // 返回手动注销逻辑
        } else {
            OnBackPressedCallback backStackCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // AndroidX 的拦截是临时的，如果不执行此 remove，后续 onBackPressed 仍会被拦截
                    this.remove();
                    callback.accept(true);
                }
            };

            activity.getOnBackPressedDispatcher().addCallback(activity, backStackCallback);

        }
    }

    /**
     * 设置状态栏背景颜色（兼容 Android 各版本）
     *
     * @param activity Activity
     * @param color    状态栏颜色
     */
    public static void setWindowsStatusBarColor(Activity activity, @ColorInt int color) {
        Window window = activity.getWindow();
        // 允许绘制系统栏背景
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // 取消半透明，否则颜色不会正确覆盖
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.setStatusBarColor(color);
    }


    /**
     * 设置状态栏图标是否为深色
     *
     * @param activity 当前 Activity
     * @param light    true = 深色图标(适用于浅色背景), false = 浅色图标(适用于深色背景)
     */
    public static void setWindowsStatusLight(Activity activity, boolean light) {
        Window window = activity.getWindow();

        // Android 11+ (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                if (light) {
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                } else {
                    controller.setSystemBarsAppearance(
                            0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            }
            return;
        }

        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (light) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decor.setSystemUiVisibility(flags);
    }

    public static QMUIPopup showMessagePopup(View view, Context context, String message) {
        int screenWidth = QMUIDisplayHelper.getScreenWidth(context);
        int halfWidth = screenWidth / 2;
        return showMessagePopup(view, context, message, halfWidth);
    }

    public static QMUIPopup showMessagePopup(View view, Context context, String message, Integer width) {
        TextView textView = new TextView(context);
        textView.setLineSpacing(QMUIDisplayHelper.dp2px(context, 4), 1.0f);
        int padding = QMUIDisplayHelper.dp2px(context, 10);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(message);
        return QMUIPopups.popup(context, width)
                .preferredDirection(QMUIPopup.DIRECTION_BOTTOM)
                .view(textView)
                .skinManager(QMUISkinManager.defaultInstance(context))
                .edgeProtection(QMUIDisplayHelper.dp2px(context, 20))
                .offsetX(QMUIDisplayHelper.dp2px(context, 20))
                .offsetYIfBottom(QMUIDisplayHelper.dp2px(context, 5))
                .shadow(true)
                .arrow(true)
                .animStyle(QMUIPopup.ANIM_GROW_FROM_CENTER)
                .show(view);
    }

    public static QMUIPopup listPopup(Context context, int width, int maxHeight, BaseAdapter adapter, AdapterView.OnItemClickListener onItemClickListener, AdapterView.OnItemLongClickListener longClickListener) {
        ListView listView = new QMUIWrapContentListView(context, maxHeight);
        listView.setAdapter(adapter);
        listView.setVerticalScrollBarEnabled(false);
        listView.setOnItemClickListener(onItemClickListener);
        listView.setOnItemLongClickListener(longClickListener);
        listView.setDivider(null);
        listView.setPadding(10, 10, 10, 10);
        return QMUIPopups.popup(context, width).view(listView);
    }

    public static void requestFocusAndShowInput(Context context, View view) {
        requestFocusAndShowInput(context, view, null);
    }

    public static void requestFocusAndShowInput(Context context, View view, Runnable runnable) {
        if (view == null) {
            return;
        }

        // 使用 post 确保 View 已经完成布局并可以接收焦点
        view.post(() -> {
            // 1. 请求焦点
            view.requestFocus();
            // 2. 类型检查：如果是 EditText，则处理光标位置
            if (view instanceof EditText) {
                EditText editText = (EditText) view;
                Editable text = editText.getText();
                if (text != null) {
                    // 将光标移动到文本末尾
                    editText.setSelection(text.length());
                }
            }
            // 3. 显示软键盘
            showInput(context, view);
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    /**
     * 辅助方法：只负责呼出键盘
     */
    public static void showInput(Context context, View view) {
        if (context == null || view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // 使用 SHOW_IMPLICIT 是最标准的选择
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static void onOpenFail(Context context, int type, int code, Runnable runnable, Object... args) {
        ThreadUtil.runOnUi(() -> {
            if (ScriptEngine.ResultCallback.hasFlags(code, ScriptEngine.ResultCallback.ACCESSIBLE, ScriptEngine.ResultCallback.JUMP)) {
                Toast.makeText(context, "已跳转到无障碍设置!", Toast.LENGTH_SHORT).show();
            } else if (ScriptEngine.ResultCallback.hasFlags(code, ScriptEngine.ResultCallback.JUMP, ScriptEngine.ResultCallback.FLOATING)) {
                Toast.makeText(context, "已跳转到悬浮窗设置!", Toast.LENGTH_SHORT).show();
            } else if (ScriptEngine.ResultCallback.hasFlags(code, ScriptEngine.ResultCallback.ROOT, ScriptEngine.ResultCallback.EXCEPTION)) {
                //root模式且发送异常
                boolean hasError = false;
                for (Object arg : args) {
                    if (arg instanceof DeviceNotRootedException) {
                        Toast.makeText(context, "当前设备未root!", Toast.LENGTH_SHORT).show();
                        hasError = true;
                    } else if (arg instanceof UnauthorizedRootAccessException) {
                        Toast.makeText(context, "当前应用未授权root!", Toast.LENGTH_SHORT).show();
                        hasError = true;
                    }
                }
                if (hasError && type == SettingProxy.AUTO) {
                    runnable.run();
                }
            } else if (ScriptEngine.ResultCallback.hasFlags(code, ScriptEngine.ResultCallback.CANCEL)) {
                Toast.makeText(context, "已取消!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "打开失败!", Toast.LENGTH_SHORT).show();
                for (Object arg : args) {
                    if (arg instanceof Exception) {
                        Log.e("ActivityUtils", "error:" + code, (Exception) arg);
                    }
                }
            }
        });
    }

    public static String toReadable(Context context, Long v, TimeUnit timeUnit, TimeUnit[] units, Integer index) {
        //todo
        return "";
    }
}
