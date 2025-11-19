package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.utils.ActivityUtils;

/**
 * 主页面
 */
public class DisclaimerActivity extends AppCompatActivity {

    private Button btnAccept;
    private Setting setting;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);
        //设置标题
        ActivityUtils.showBar(this, R.id.disclaimerTitle);
        //获取配置
        setting = BeanFactory.getInstance().get(Setting.class);
        if (setting.isAccepted()) {
            jump();
            return;
        }
        btnAccept = findViewById(R.id.btnAccept);
        Button btnDecline = findViewById(R.id.btnDecline);
        btnAccept.setOnClickListener((e) -> this.accept());
        btnDecline.setOnClickListener(e -> this.decline());
        //启动倒计时
        startCountDown();
    }

    private void startCountDown() {
        // 倒计时
        timer = new CountDownTimer(setting.getTick() * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnAccept.setText(getString(R.string.btn_accept_count,
                        getString(R.string.btn_accept),
                        millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                btnAccept.setText(R.string.btn_accept);
                accept();
            }
        }.start();
    }

    public void accept() {
        setting.setAccepted(true);
        timer.cancel();
        jump();
    }

    public void decline() {
        timer.cancel();
        //提示退出
        Toast.makeText(this, "您已拒绝协议，应用即将退出", Toast.LENGTH_SHORT).show();

        // 延迟退出（避免 Toast 还没显示就退出了）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }, 1000);
    }

    private void jump() {
        Intent intent = new Intent(DisclaimerActivity.this, MacroListActivity.class);
        startActivity(intent);
        finish();
    }
}
