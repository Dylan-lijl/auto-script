package pub.carzy.auto_script.model;


import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.db.entity.AiChatAppDataEntity;

public class AiChatAppDataModel extends BaseObservable {
    private boolean requesting = false;
    private boolean show = false;
    private final List<AiChatAppDataEntity> data = new ArrayList<>();

    public AiChatAppDataEntity lastEntity() {
        return data.isEmpty() ? null : data.get(data.size() - 1);
    }

    @Bindable
    public boolean isRequesting() {
        return requesting;
    }

    public void setRequesting(boolean requesting) {
        this.requesting = requesting;
        notifyPropertyChanged(BR.requesting);
    }

    @Bindable
    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
        notifyPropertyChanged(BR.show);
    }

    @Bindable
    public List<AiChatAppDataEntity> getData() {
        return data;
    }
}
