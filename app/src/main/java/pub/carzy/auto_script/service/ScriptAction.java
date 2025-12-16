package pub.carzy.auto_script.service;

import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;
import pub.carzy.auto_script.service.dto.UpdateParam;

/**
 * @author admin
 */
public interface ScriptAction {
    default int order() {
        return Integer.MAX_VALUE;
    }

    String key();
    default String group() {
        return "default";
    }

    boolean open(OpenParam param);

    boolean close(CloseParam param);

    void setContext(MyAccessibilityService service);

    void onAccessibilityEvent(AccessibilityEvent event);

    boolean onKeyEvent(KeyEvent event);

    void onInterrupt();
    boolean update(UpdateParam param);
}
