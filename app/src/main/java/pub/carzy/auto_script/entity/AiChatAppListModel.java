package pub.carzy.auto_script.entity;

import android.app.Activity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ViewDataBinding;

import java.util.function.BiFunction;
import java.util.function.Function;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.ext.ai_abs.AiChatLifeCycle;

public class AiChatAppListModel<C extends AiChatAppBaseConfig,V extends ViewDataBinding> extends BaseObservable {
    public static <C extends AiChatAppBaseConfig,V extends ViewDataBinding> AiChatAppListModel<C, V> create(AiChatLifeCycle<C, V> cycle) {
        AiChatAppListModel<C, V> model = new AiChatAppListModel<>();
        model.setLifeCycle(cycle);
        model.setName(cycle.name());
        model.setKey(cycle.key());
        model.setConfig(cycle.getConfig());
        model.setCreateView(cycle.createView());
        return model;
    }

    private String name;
    private String key;
    private Boolean enable = false;
    private BiFunction<AiChatAppListModel<C, V>, Activity, V> createView;
    private AiChatLifeCycle<C, V> lifeCycle;

    private C config;

    @Bindable
    public C getConfig() {
        return config;
    }

    public void setConfig(C config) {
        this.config = config;
        notifyPropertyChanged(BR.config);
    }

    @Bindable
    public AiChatLifeCycle<C, V> getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(AiChatLifeCycle<C, V> lifeCycle) {
        this.lifeCycle = lifeCycle;
        notifyPropertyChanged(BR.lifeCycle);
    }

    @Bindable
    public BiFunction<AiChatAppListModel<C, V>, Activity, V> getCreateView() {
        return createView;
    }

    public void setCreateView(BiFunction<AiChatAppListModel<C, V>, Activity, V> createView) {
        this.createView = createView;
        notifyPropertyChanged(BR.createView);
    }


    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }

    @Bindable
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        notifyPropertyChanged(BR.key);
    }

    @Bindable
    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
        notifyPropertyChanged(BR.enable);
    }

}
