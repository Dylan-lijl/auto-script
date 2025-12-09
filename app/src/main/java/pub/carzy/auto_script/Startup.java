package pub.carzy.auto_script;

import android.app.Application;
import android.content.Context;

import pub.carzy.auto_script.config.BeanRegister;

/**
 * @author admin
 */
public class Startup extends Application {
    @Override
    public void onCreate() {
        BeanRegister.run(this);
        super.onCreate();
    }
}
