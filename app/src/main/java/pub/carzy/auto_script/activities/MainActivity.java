package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;

/**
 * 主页面
 * @author admin
 */
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取配置
        Setting setting = BeanFactory.getInstance().get(Setting.class);
        if (setting.isAccepted()) {
            // 已同意，跳转到列表页面
            startActivity(new Intent(this, MacroListActivity.class));
        } else {
            // 未同意，跳转到免责声明页面
            startActivity(new Intent(this, DisclaimerActivity.class));
        }
        finish();
    }

}
