package pub.carzy.auto_script.ext.impl;

import android.app.Activity;
import android.view.LayoutInflater;

import java.util.function.BiFunction;
import java.util.function.Function;

import pub.carzy.auto_script.databinding.ComAiChatAppConfigQqBinding;
import pub.carzy.auto_script.databinding.ComAiChatAppConfigWechatBinding;
import pub.carzy.auto_script.entity.AiChatAppListModel;
import pub.carzy.auto_script.ext.ai_abs.AbstractAiChatLifeCycle;
import pub.carzy.auto_script.ext.entities.WechatAiChatConfig;

public class WechatAiChatLifeCycle extends AbstractAiChatLifeCycle<WechatAiChatConfig, ComAiChatAppConfigWechatBinding> {
    @Override
    public String key() {
        return "wechat";
    }

    @Override
    public String name() {
        return "微信";
    }

    @Override
    public BiFunction<AiChatAppListModel<WechatAiChatConfig, ComAiChatAppConfigWechatBinding>, Activity, ComAiChatAppConfigWechatBinding> createView() {
        return (m, a) -> {
            ComAiChatAppConfigWechatBinding b = ComAiChatAppConfigWechatBinding.inflate(LayoutInflater.from(a));
            b.setModel(m.getConfig());
            b.setTitle(name());
            return b;
        };
    }

    @Override
    protected WechatAiChatConfig createDefaultConfig() {
        WechatAiChatConfig config = new WechatAiChatConfig();
        config.init();
        return config;
    }
}
