package pub.carzy.auto_script.entity;

import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.db.entity.AiChatAppDataEntity;

public class AiChatAppDataAggEntity extends AiChatAppDataEntity {
    private Integer count = 0;

    @Bindable
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
        notifyPropertyChanged(BR.count);
    }
}
