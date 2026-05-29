package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import pub.carzy.auto_script.BR;

public class AiChatApiModel extends BaseObservable {
    private String id;
    private Long created;
    private Object object_;
    private String ownedBy;
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @Bindable
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        notifyPropertyChanged(BR.id);
    }

    @Bindable
    public String getCreatedTime() {
        if (created != null) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(created));
        }
        return "";
    }

    @Bindable
    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
        notifyPropertyChanged(BR.created);
        notifyPropertyChanged(BR.createdTime);
    }

    @Bindable
    public Object getObject_() {
        return object_;
    }

    public void setObject_(Object object_) {
        this.object_ = object_;
        notifyPropertyChanged(BR.object_);
    }

    @Bindable
    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
        notifyPropertyChanged(BR.ownedBy);
    }

    @Bindable
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
}
