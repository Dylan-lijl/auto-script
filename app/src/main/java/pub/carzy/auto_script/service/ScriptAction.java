package pub.carzy.auto_script.service;

import android.view.accessibility.AccessibilityEvent;

import pub.carzy.auto_script.service.dto.CloseParam;
import pub.carzy.auto_script.service.dto.OpenParam;

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

    public void onAccessibilityEvent(AccessibilityEvent event);

    public void onInterrupt();
}
