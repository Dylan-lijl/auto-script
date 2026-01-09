package pub.carzy.auto_script.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.dto.UpdateParam;
import pub.carzy.auto_script.service.impl.RecordScriptAction;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class MyAccessibilityService extends AccessibilityService {
    private final Map<String, LinkedHashSet<ScriptAction>> groupActions;
    private final Map<String, String> keys;
    private final Map<String, ScriptAction> opens;
    private final AtomicBoolean initing;
    private Locale locale;
    private Setting setting;

    private final BroadcastReceiver screenReceiver;

    public MyAccessibilityService() {
        groupActions = new LinkedHashMap<>();
        initing = new AtomicBoolean(false);
        opens = new LinkedHashMap<>();
        keys = new HashMap<>();
        initing.set(true);
        BeanFactory.getInstance().register(this);
        ThreadUtil.runOnCpu(() -> {
            //加载实现类
            ServiceLoader<ScriptAction> load = ServiceLoader.load(ScriptAction.class);
            for (ScriptAction action : load) {
                keys.put(action.key(), action.group());
                groupActions.computeIfAbsent(action.group(), k -> new LinkedHashSet<>()).add(action);
            }
            initing.set(false);
        });
        setting = BeanFactory.getInstance().get(Setting.class);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //传递到已开启的组件
                opens.forEach((k, v) -> {
                    try {
                        v.screenChanged(action);
                    } catch (Exception e) {
                        Log.e(this.getClass().getCanonicalName(), "screenChanged", e);
                    }
                });
            }
        };
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        locale = ActivityUtils.getLocale(newBase, setting);
        super.attachBaseContext(ActivityUtils.updateLocale(newBase, locale));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        opens.forEach((k, v) -> v.onAccessibilityEvent(event));
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //由于service.getSystemService()需要在create之后,所以放在这里设置
        groupActions.values().forEach(v -> v.forEach(item -> {
            try {
                item.setContext(this);
            } catch (Exception e) {
                Log.e(this.getClass().getCanonicalName(), "MyAccessibilityService()", e);
            }
        }));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(screenReceiver);
        return super.onUnbind(intent);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        opens.forEach((k, v) -> v.onKeyEvent(event));
        return super.onKeyEvent(event);
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
                return action.open(param);
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

    public boolean update(String key, UpdateParam param) {
        if (key == null) {
            Log.e("update", "key is null");
            return false;
        }
        if (!keys.containsKey(key)) {
            Log.e("update", "key is not exist");
            return false;
        }
        String group = keys.get(key);
        LinkedHashSet<ScriptAction> set = groupActions.get(group);
        if (set == null) {
            Log.e("update", "group is not exist");
            return false;
        }
        ScriptAction item = opens.get(group);
        if (item == null || !item.key().equals(key)) {
            Log.e("update", "same key is not same object");
            return false;
        }
        try {
            return item.update(param);
        } catch (Exception e) {
            Log.e("update", "update error", e);
        }
        return false;
    }

}
