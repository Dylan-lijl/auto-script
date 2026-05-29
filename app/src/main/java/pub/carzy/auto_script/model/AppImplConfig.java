package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;

public class AppImplConfig extends BaseObservable {
    /**
     * 平台类型
     */
    private Integer type;
    /**
     * 平台名称
     */
    private String app;
    /**
     * 匹配规则
     */
    private String match;
    /**
     * 采集基础信息
     */
    private Boolean collectable = false;
    /**
     * 提示词
     */
    private String prompt;
    /**
     * 白名单
     */
    private List<String> whitelist = new ArrayList<>();
    /**
     * 黑名单
     */
    private List<String> blacklist = new ArrayList<>();

    @Bindable
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
        notifyPropertyChanged(BR.type);
    }

    @Bindable
    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
        notifyPropertyChanged(BR.app);
    }

    @Bindable
    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
        notifyPropertyChanged(BR.match);
    }

    @Bindable
    public Boolean getCollectable() {
        return collectable;
    }

    public void setCollectable(Boolean collectable) {
        this.collectable = collectable;
        notifyPropertyChanged(BR.collectable);
    }

    @Bindable
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        notifyPropertyChanged(BR.prompt);
    }

    @Bindable
    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
        notifyPropertyChanged(BR.whitelist);
    }

    @Bindable
    public List<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<String> blacklist) {
        this.blacklist = blacklist;
        notifyPropertyChanged(BR.blacklist);
    }
}
