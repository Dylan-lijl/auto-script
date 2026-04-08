package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import pub.carzy.auto_script.config.BeanContainer;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.config.pojo.SettingKey;

/**
 * 主页面
 *
 * @author admin
 */
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //检查是否同意免责声明
        Setting setting = BeanContainer.getInstance().get(Setting.class);
        if (setting.read(SettingKey.ACCEPTED, false)) {
            // 已同意，跳转到列表页面
            startActivity(new Intent(this, MacroListActivity.class));
        } else {
            // 未同意，跳转到免责声明页面
            startActivity(new Intent(this, DisclaimerActivity.class));
        }
        finish();
    }

}
