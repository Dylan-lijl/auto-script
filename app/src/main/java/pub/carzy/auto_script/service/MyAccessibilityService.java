package pub.carzy.auto_script.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.databinding.ViewDataBinding;

import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public abstract class MyAccessibilityService extends AccessibilityService {
    private final Map<String, LinkedHashSet<ScriptAction>> groupActions;
    private final AtomicBoolean initing ;
    public MyAccessibilityService(){
        groupActions = new LinkedHashMap<>();
        initing = new AtomicBoolean(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initing.set(true);
        ThreadUtil.runOnCpu(() -> {
            //加载实现类
            ServiceLoader<ScriptAction> load = ServiceLoader.load(ScriptAction.class);
            for (ScriptAction action : load) {
                groupActions.computeIfAbsent(action.group(), k -> new LinkedHashSet<>()).add(action);
                try {
                    action.setContext(this);
                } catch (Exception ignored) {
                }
            }
            initing.set(false);
        });
    }
}
