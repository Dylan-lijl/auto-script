package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.entity.AiChatApiModel;
import pub.carzy.auto_script.entity.AiChatConfig;

public class AiChatModel extends BaseObservable {
    private boolean running = false;
    private boolean searchingModel = false;
    private Boolean connectable;
    private AiChatConfig aiChatConfig;

    private final List<AppImplConfig> appImplConfigs = new ArrayList<>();

    private final List<AiChatApiModel> models = new ArrayList<>();

    @Bindable
    public List<AiChatApiModel> getModels() {
        return models;
    }

    @Bindable
    public boolean isSearchingModel() {
        return searchingModel;
    }

    public void setSearchingModel(boolean searchingModel) {
        this.searchingModel = searchingModel;
        notifyPropertyChanged(BR.searchingModel);
    }

    @Bindable
    public List<AppImplConfig> getAppImplConfigs() {
        return appImplConfigs;
    }

    @Bindable
    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        notifyPropertyChanged(BR.running);
    }

    @Bindable
    public AiChatConfig getAiChatConfig() {
        return aiChatConfig;
    }

    public void setAiChatConfig(AiChatConfig aiChatConfig) {
        this.aiChatConfig = aiChatConfig;
        notifyPropertyChanged(BR.aiChatConfig);
    }

}
