package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.controller.DisclaimerController;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 主页面
 *
 * @author admin
 */
public class DisclaimerActivity extends BaseActivity {

    private Button btnAccept;
    private CountDownTimer timer;
    private static DisclaimerController CONTROLLER;

    @Override
    protected Integer getActionBarTitle() {
        return R.string.disclaimers_title;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CONTROLLER = new DisclaimerController();
        setContentView(R.layout.activity_disclaimer);
        btnAccept = findViewById(R.id.btnAccept);
        Button btnDecline = findViewById(R.id.btnDecline);
        //获取配置
        CONTROLLER.isAccepted((isAccepted) -> {
            if (isAccepted) {
                jump();
                return;
            }
            btnAccept.setOnClickListener((e) -> this.accept());
            btnDecline.setOnClickListener(e -> this.decline());
            //启动倒计时
            startCountDown();
        });
    }

    private void startCountDown() {
        // 倒计时
        CONTROLLER.getTick((tick) -> timer = new CountDownTimer(tick * 1000, 1000) {
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
        }.start());
    }

    public void accept() {
        CONTROLLER.setAccepted((r) -> {
            timer.cancel();
            jump();
        });
    }

    public void decline() {
        timer.cancel();
        //提示退出
        Toast.makeText(this, R.string.decline_exit_message, Toast.LENGTH_SHORT).show();

        // 延迟退出（避免 Toast 还没显示就退出了）
        ThreadUtil.runOnUi(() -> {
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
