package pub.carzy.auto_script;

import android.app.Application;
import android.content.Context;

import com.qmuiteam.qmui.skin.QMUISkinManager;

import pub.carzy.auto_script.config.BeanRegister;

/**
 * 启动类
 * @author admin
 */
public class Startup extends Application {
    @Override
    public void onCreate() {
        //添加全局必要组件
        BeanRegister.run(this);
        super.onCreate();
    }
}
