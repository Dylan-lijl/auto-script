package pub.carzy.auto_script.controller;

import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;
import pub.carzy.auto_script.config.Setting;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class DisclaimerController extends AbstractController{
    private final Setting setting;

    public DisclaimerController() {
        setting = BeanFactory.getInstance().get(Setting.class);
    }

    public void isAccepted(ControllerCallback<Boolean> callback) {
        ThreadUtil.runOnCpu(() -> {
            boolean accepted = setting.isAccepted();
            try {
                ThreadUtil.runOnUi(() -> callback.complete(accepted));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    public void getTick(ControllerCallback<Integer> callback) {
        ThreadUtil.runOnCpu(() -> {
            Integer tick = setting.getTick();
            try {
                ThreadUtil.runOnUi(() -> callback.complete(tick));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }

    public void setAccepted(ControllerCallback<Void> callback) {
        ThreadUtil.runOnCpu(() -> {
            setting.setAccepted(true);
            try {
                ThreadUtil.runOnUi(() -> callback.complete(null));
            } catch (Exception e) {
                ThreadUtil.runOnUi(() -> callback.catchMethod(e));
            } finally {
                ThreadUtil.runOnUi(callback::finallyMethod);
            }
        });
    }
}
