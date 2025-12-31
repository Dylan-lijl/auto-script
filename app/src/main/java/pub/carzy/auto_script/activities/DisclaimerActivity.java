package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.controller.DisclaimerController;
import pub.carzy.auto_script.databinding.ViewDisclaimerBinding;
import pub.carzy.auto_script.model.DisclaimerCount;
import pub.carzy.auto_script.model.DisclaimerStatus;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 主页面
 *
 * @author admin
 */
public class DisclaimerActivity extends BaseActivity {

    private CountDownTimer timer;
    private static DisclaimerController CONTROLLER;
    private DisclaimerStatus status;
    private DisclaimerCount counter;
    private ViewDisclaimerBinding binding;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.disclaimers_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        status = new DisclaimerStatus();
        counter = new DisclaimerCount();
        binding = DataBindingUtil.setContentView(this, R.layout.view_disclaimer);
        binding.setStatus(status);
        binding.setCounter(counter);
        CONTROLLER = new DisclaimerController();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //获取配置
        CONTROLLER.isAccepted((isAccepted) -> {
            if (isAccepted) {
                jump();
                return;
            }
            binding.btnAccept.setOnClickListener((e) -> this.accept());
            binding.btnDecline.setOnClickListener(e -> this.decline());
            //启动倒计时
            startCountDown();
        });
    }

    private void startCountDown() {
        // 倒计时
        CONTROLLER.getTick((tick) -> {
            counter.setTick(tick);
            timer = new CountDownTimer(tick * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    counter.setTick((int) (millisUntilFinished / 1000));
                }

                @Override
                public void onFinish() {
                    decline();
                }
            }.start();
        });
    }

    public void accept() {
        status.setAccepting(true);
        stopTimer();
        CONTROLLER.setAccepted(new ControllerCallback<>() {
            @Override
            public void complete(Void result) {
                timer.cancel();
                jump();
            }

            @Override
            public void finallyMethod() {
                status.setAccepting(false);
            }
        });
    }

    public void decline() {
        status.setDeclining(true);
        stopTimer();
        //提示退出
        Toast.makeText(this, R.string.decline_exit_message, Toast.LENGTH_SHORT).show();

        // 延迟退出（避免 Toast 还没显示就退出了）
        ThreadUtil.runOnUi(() -> {
            status.setDeclining(false);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }, 1000);
    }

    private void stopTimer() {
        timer.cancel();
        counter.setTick(0);
    }

    private void jump() {
        Intent intent = new Intent(DisclaimerActivity.this, MacroListActivity.class);
        startActivity(intent);
        finish();
    }
}
