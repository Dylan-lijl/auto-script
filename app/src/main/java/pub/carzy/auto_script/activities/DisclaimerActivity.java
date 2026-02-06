package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.AbstractSetting;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.databinding.ViewDisclaimerBinding;
import pub.carzy.auto_script.model.DisclaimerModel;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 主页面
 *
 * @author admin
 */
public class DisclaimerActivity extends BaseActivity {

    private CountDownTimer timer;
    private ViewDisclaimerBinding binding;
    private DisclaimerModel model;
    private Setting setting;

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.disclaimers_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_disclaimer);
        model = new DisclaimerModel();
        binding.setModel(model);
        setting = BeanFactory.getInstance().get(Setting.class);
        initTopBar();
        init();
    }
    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }
    @Override
    protected void initTopBar() {
        super.initTopBar();
    }

    @Override
    protected void openBottomSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                .setGravityCenter(false)
                .setAddCancelBtn(false)
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    if (tag == null) {
                        return;
                    }
                    int id = ActionInflater.ActionItem.stringToId(tag);
                    if (defaultProcessMenu(id)) {
                        return;
                    }
                });
        if (model.isView()) {
            //如果只是参观模式则不过滤菜单
            addDefaultMenu(builder);
        } else {
            //否则过滤只保留切换语言功能
            addActionByXml(builder, this, R.xml.actions_common,
                    (b, model, item) -> {
                        //只保留切换语言
                        if (item.getId() != R.id.menu_language) {
                            return;
                        }
                        b.addItem(model);
                    });
        }
        QMUIBottomSheet build = builder.build();
        build.show();
    }

    private void init() {
        ThreadUtil.runOnCpu(() -> {
            boolean accepted = setting.isAccepted();
            ThreadUtil.runOnUi(() -> {
                //如果已经是同意状态下切换观看模式
                model.setView(accepted);
                //非同意下才开始倒计时
                if (!accepted) {
                    startCountDown();
                }
            });
        });
        binding.btnAccept.setOnClickListener((e) -> this.accept());
        binding.btnDecline.setOnClickListener(e -> this.decline());
    }

    /**
     * 启动倒计时,为结束前不能点同意,保证用户查看过内容
     */
    private void startCountDown() {
        //倒计时
        ThreadUtil.runOnCpu(() -> {
            Integer tick = MixedUtil.getValueOrDefault(setting.getTick(), AbstractSetting.DefaultValue.TICK);
            ThreadUtil.runOnUi(() -> {
                model.setTick(tick);
                timer = new CountDownTimer(tick * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        model.setTick((int) (millisUntilFinished / 1000));
                    }

                    @Override
                    public void onFinish() {

                    }
                }.start();
            });
        });
    }

    public void accept() {
        //倒计时未结束
        if (model.getTick() > 0) {
            return;
        }
        model.setAccepting(true);
        ThreadUtil.runOnCpu(() -> {
            //更新配置
            setting.setAccepted(true);
            ThreadUtil.runOnUi(() -> {
                model.setAccepting(false);
                //跳转
                jump();
            });
        });
    }

    public void decline() {
        model.setDeclining(true);
        timer.cancel();
        //提示退出
        Toast.makeText(this, R.string.decline_exit_message, Toast.LENGTH_SHORT).show();
        // 延迟退出（避免 Toast 还没显示就退出了）
        ThreadUtil.runOnUi(() -> {
            model.setDeclining(false);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }, 1000);
    }

    /**
     * 跳回到主页面
     */
    private void jump() {
        Intent intent = new Intent(DisclaimerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
