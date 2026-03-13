package pub.carzy.auto_script;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.qmuiteam.qmui.skin.QMUISkinManager;

import pub.carzy.auto_script.config.BeanRegister;

/**
 * 启动类
 * @author admin
 */
public class Startup extends Application {
    public static Activity CURRENT;

    @Override
    public void onCreate() {
        //获取顶层activity
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                CURRENT = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (CURRENT == activity) {
                    CURRENT = null;
                }
            }

            // ... 其他回调空实现
            @Override public void onActivityCreated(@NonNull Activity activity, Bundle s) {}
            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle o) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
        //添加全局必要组件
        BeanRegister.run(this);
        super.onCreate();
    }
}
