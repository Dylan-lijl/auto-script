package pub.carzy.auto_script.db.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Date;

import pub.carzy.auto_script.BR;

public class AiChatAppDataEntity extends BaseObservable {
    private Long id;
    private Long parentId;
    private Integer key;
    private String name;
    private String nickname;
    private String sessionId;
    private Integer type;
    private String otherData;
    private Date createTime;
    private Date updateTime;

    @Bindable
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
        notifyPropertyChanged(BR.parentId);
    }

    @Bindable
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
        notifyPropertyChanged(BR.createTime);
    }

    @Bindable
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        notifyPropertyChanged(BR.updateTime);
    }

    @Bindable
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
        notifyPropertyChanged(BR.id);
    }

    @Bindable
    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
        notifyPropertyChanged(BR.key);
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
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        notifyPropertyChanged(BR.nickname);
    }

    @Bindable
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        notifyPropertyChanged(BR.sessionId);
    }

    @Bindable
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
        notifyPropertyChanged(BR.type);
    }

    @Bindable
    public String getOtherData() {
        return otherData;
    }

    public void setOtherData(String otherData) {
        this.otherData = otherData;
        notifyPropertyChanged(BR.otherData);
    }
}
