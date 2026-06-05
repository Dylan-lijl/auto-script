package pub.carzy.auto_script.entity;


import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * 通用配置
 */
public class AiChatAppBaseConfig extends BaseObservable {
    /**
     * 是否启用
     */
    private Boolean enable;
    /**
     * ai提示词
     */
    private String prompt;
    /**
     * 启用提示词
     */
    private Boolean enablePrompt = false;
    @Bindable
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        notifyPropertyChanged(BR.prompt);
    }

    @Bindable
    public Boolean getEnablePrompt() {
        return enablePrompt;
    }

    public void setEnablePrompt(Boolean enablePrompt) {
        this.enablePrompt = enablePrompt;
        notifyPropertyChanged(BR.enablePrompt);
    }

    @Bindable
    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
        notifyPropertyChanged(BR.enable);
    }
    public void init(){
        setEnable(false);
        setEnablePrompt(true);
        setPrompt("");
    }
}
