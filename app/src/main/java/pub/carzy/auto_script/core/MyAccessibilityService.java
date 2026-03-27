package pub.carzy.auto_script.core;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.Locale;
import lombok.Setter;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.utils.ActivityUtils;

/**
 * @author admin
 */
public class MyAccessibilityService extends AccessibilityService {
    private final Setting setting;
    @Setter
    private EventCallback callback;
    private final BroadcastReceiver screenReceiver;

    public MyAccessibilityService() {
        BeanFactory.getInstance().register(this);
        setting = BeanFactory.getInstance().get(Setting.class);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (callback != null) {
                    callback.onActionEvent(intent.getAction());
                }
            }
        };
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale locale = ActivityUtils.getLocale(newBase, setting);
        super.attachBaseContext(ActivityUtils.updateLocale(newBase, locale));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (callback != null) {
            callback.onAccessibilityEvent(event);
        }
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
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(screenReceiver);
        return super.onUnbind(intent);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (callback != null) {
            callback.onKeyEvent(event);
        }
        return super.onKeyEvent(event);
    }


    public interface EventCallback {
        default void onActionEvent(String action) {

        }

        default void onKeyEvent(KeyEvent event) {

        }

        default void onAccessibilityEvent(AccessibilityEvent event) {

        }
    }

}
