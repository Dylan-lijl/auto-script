package pub.carzy.auto_script.service;

import android.accessibilityservice.AccessibilityService;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.impl.RecordScriptAction;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MyAccessibilityService extends AccessibilityService {
    private final Map<String, LinkedHashSet<ScriptAction>> groupActions;
    private final Map<String, String> keys;
    private final Map<String, ScriptAction> opens;
    private final AtomicBoolean initing;

    public MyAccessibilityService() {
        groupActions = new LinkedHashMap<>();
        initing = new AtomicBoolean(false);
        opens = new LinkedHashMap<>();
        keys = new HashMap<>();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        initing.set(true);
        BeanFactory.getInstance().register(this);
        ThreadUtil.runOnCpu(() -> {
            //加载实现类
            ServiceLoader<ScriptAction> load = ServiceLoader.load(ScriptAction.class);
            for (ScriptAction action : load) {
                keys.put(action.key(), action.group());
                groupActions.computeIfAbsent(action.group(), k -> new LinkedHashSet<>()).add(action);
                try {
                    action.setContext(this);
                } catch (Exception ignored) {
                }
            }
            initing.set(false);
        });
    }

    public boolean open(String key, OpenParam param) {
        if (key == null) {
            Log.e("open", "key is null");
            return false;
        }
        if (!keys.containsKey(key)) {
            Log.e("open", "key is not exist");
            return false;
        }
        String group = keys.get(key);
        LinkedHashSet<ScriptAction> set = groupActions.get(group);
        if (set == null) {
            Log.e("open", "group is not exist");
            return false;
        }
        ScriptAction old = opens.get(group);
        if (old != null && key.equals(old.key())) {
            return true;
        } else if (old != null) {
            old.close(null);
        }
        for (ScriptAction action : set) {
            if (key.equals(action.key())) {
                opens.put(group, action);
                action.open(param);
                return true;
            }
        }
        return false;
    }

    public boolean close(String key, CloseParam param) {
        String group = keys.get(key);
        if (group != null) {
            ScriptAction old = opens.get(group);
            if (old != null && old.key().equals(key)) {
                old.close(param);
                opens.remove(group);
                return true;
            }
        }
        return false;
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
