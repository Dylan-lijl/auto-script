package pub.carzy.auto_script.ext.ai_abs;

import androidx.databinding.ViewDataBinding;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import pub.carzy.auto_script.entity.AiChatAppBaseConfig;

public class AiChatAppManager {
    public static final Map<String, AiChatLifeCycle<? extends AiChatAppBaseConfig,? extends ViewDataBinding>> apps = new HashMap<>();

    public static synchronized void register(AiChatLifeCycle<? extends AiChatAppBaseConfig,? extends ViewDataBinding> app) {
        if (app == null) {
            return;
        }
        register(app.key(), app);
    }

    public static synchronized void register(String key, AiChatLifeCycle<? extends AiChatAppBaseConfig,? extends ViewDataBinding> app) {
        if (app == null) {
            return;
        }
        apps.remove(key);
        apps.put(key, app);
    }

    public static synchronized void unregister(String key) {
        apps.remove(key);
    }

    public static synchronized Map<String, AiChatLifeCycle<? extends AiChatAppBaseConfig,? extends ViewDataBinding>> all() {
        return apps.values().stream().sorted(Comparator.comparing(AiChatLifeCycle::key)).collect(Collectors.toMap(AiChatLifeCycle::key, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public static synchronized AiChatLifeCycle<? extends AiChatAppBaseConfig,? extends ViewDataBinding> get(String key) {
        return apps.get(key);
    }

    public static synchronized boolean has(String key) {
        return apps.containsKey(key);
    }

    public static synchronized boolean has(AiChatLifeCycle<? extends AiChatAppBaseConfig,? extends ViewDataBinding> item) {
        return apps.containsValue(item);
    }
}
