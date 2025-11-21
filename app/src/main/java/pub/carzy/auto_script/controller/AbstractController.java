package pub.carzy.auto_script.controller;

import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.Setting;

/**
 * @author admin
 */
public abstract class AbstractController {
    protected final Setting setting;

    public AbstractController() {
        setting = BeanFactory.getInstance().get(Setting.class);
    }
}
