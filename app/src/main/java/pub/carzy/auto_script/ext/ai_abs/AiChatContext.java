package pub.carzy.auto_script.ext.ai_abs;

import pub.carzy.auto_script.config.Setting;

public class AiChatContext {
    public AiChatContext(Setting setting) {
        this.setting = setting;
    }

    private final Setting setting;

    public Setting getSetting() {
        return setting;
    }
}
