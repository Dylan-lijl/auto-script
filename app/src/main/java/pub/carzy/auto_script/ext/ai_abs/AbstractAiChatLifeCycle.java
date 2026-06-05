package pub.carzy.auto_script.ext.ai_abs;

import static pub.carzy.auto_script.config.pojo.SettingKey.AI_CHAT_APP;

import androidx.databinding.ViewDataBinding;

import java.lang.reflect.ParameterizedType;

import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.entity.AiChatAppBaseConfig;

public abstract class AbstractAiChatLifeCycle<C extends AiChatAppBaseConfig,V extends ViewDataBinding> implements AiChatLifeCycle<C,V> {

    protected AiChatContext context;

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    public AiChatLifeCycle<C, V> setContext(AiChatContext context) {
        this.context = context;
        return this;
    }

    @Override
    public C getConfig() {
        String key = AI_CHAT_APP.getKey() + key();
        C config = context.getSetting().read(new SettingKey<>(key, configType()), null);
        if (config == null) {
            return createDefaultConfig();
        }
        return config;
    }

    protected abstract C createDefaultConfig();

    @Override
    public void saveConfig(C t) {
        if (t == null) {
            return;
        }
        context.getSetting().write(new SettingKey<>(AI_CHAT_APP.getKey() + key(), configType()), t);
    }

    private Class<C> clazz;

    @SuppressWarnings("unchecked")
    protected synchronized Class<C> configType() {
        if (clazz != null) {
            return clazz;
        }
        return clazz = (Class<C>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
