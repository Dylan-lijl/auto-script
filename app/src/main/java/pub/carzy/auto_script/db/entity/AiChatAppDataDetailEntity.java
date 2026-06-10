package pub.carzy.auto_script.db.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Date;

import pub.carzy.auto_script.BR;

public class AiChatAppDataDetailEntity extends BaseObservable {
    private Long id;
    /**
     * 消息,使用json字符串存储,如果消息嵌套图片还有其他内容
     * [{type:'text',encapsulate:'',content:'123'},
     * {type:'image',encapsulate:'这是图片内容',content:'data/image/xxx'},
     * {type:'audio',encapsulate:'这是语音内容',content:'data/audio/xxx'}
     * ]
     */
    private String message;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 角色,用户/系统/阶段性总结
     */
    private String role;
    private Boolean visible;
    private Date createTime;
    private Date updateTime;

    @Bindable
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
        notifyPropertyChanged(BR.id);
    }

    @Bindable
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        notifyPropertyChanged(BR.message);
    }

    @Bindable
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        notifyPropertyChanged(BR.userId);
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
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
        notifyPropertyChanged(BR.role);
    }

    @Bindable
    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
        notifyPropertyChanged(BR.visible);
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
}
