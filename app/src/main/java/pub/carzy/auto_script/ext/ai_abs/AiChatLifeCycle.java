package pub.carzy.auto_script.ext.ai_abs;

import android.app.Activity;
import android.content.Context;

import androidx.databinding.ViewDataBinding;

import java.util.function.BiFunction;
import java.util.function.Function;

import pub.carzy.auto_script.entity.AiChatAppBaseConfig;
import pub.carzy.auto_script.entity.AiChatAppListModel;

public interface AiChatLifeCycle<C extends AiChatAppBaseConfig, V extends ViewDataBinding> {
    /**
     * 标识
     *
     * @return l
     */
    String key();

    /**
     * 名称
     *
     * @return n
     */
    String name();

    /**
     * 排序
     *
     * @return o
     */
    int order();

    /**
     * 设置上下文
     *
     * @param context c
     */
    AiChatLifeCycle<C, V> setContext(AiChatContext context);

    /**
     * 获取配置
     *
     * @return 配置
     */
    C getConfig();

    /**
     * 保存配置
     *
     * @param t t
     */
    void saveConfig(C t);
    /** @noinspection unchecked*/
    default void saveConfigRaw(AiChatAppBaseConfig config) {
        saveConfig((C) config);
    }
    BiFunction<AiChatAppListModel<C,V>, Activity, V> createView();
}
