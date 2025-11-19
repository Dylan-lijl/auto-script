package pub.carzy.auto_script;

import android.app.Application;

import pub.carzy.auto_script.config.BeanRegister;

public class Startup extends Application {
    @Override
    public void onCreate() {
        BeanRegister.run(this);
        super.onCreate();
    }
}
