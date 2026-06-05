package pub.carzy.auto_script.ext.impl;

import android.app.Activity;
import android.view.LayoutInflater;

import java.util.function.BiFunction;
import java.util.function.Function;

import pub.carzy.auto_script.databinding.ComAiChatAppConfigQqBinding;
import pub.carzy.auto_script.entity.AiChatAppListModel;
import pub.carzy.auto_script.ext.ai_abs.AbstractAiChatLifeCycle;
import pub.carzy.auto_script.ext.entities.QQAiChatConfig;
import pub.carzy.auto_script.ext.entities.WechatAiChatConfig;

public class QQAiChatLifeCycle extends AbstractAiChatLifeCycle<QQAiChatConfig, ComAiChatAppConfigQqBinding> {
    @Override
    public String key() {
        return "qq";
    }

    @Override
    public String name() {
        return "QQ";
    }

    @Override
    public BiFunction<AiChatAppListModel<QQAiChatConfig, ComAiChatAppConfigQqBinding>, Activity, ComAiChatAppConfigQqBinding> createView() {
        return (m,a)->{
            ComAiChatAppConfigQqBinding b = ComAiChatAppConfigQqBinding.inflate(LayoutInflater.from(a));
            b.setModel(m.getConfig());
            b.setTitle(name());
            return b;
        };
    }

    @Override
    protected QQAiChatConfig createDefaultConfig() {
        QQAiChatConfig config = new QQAiChatConfig();
        config.init();
        return config;
    }
}
