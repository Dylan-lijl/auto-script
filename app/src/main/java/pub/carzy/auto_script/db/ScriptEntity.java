package pub.carzy.auto_script.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.Date;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class ScriptEntity extends BaseObservable implements Parcelable {
    private Long id;
    private String name;
    private Date createTime;
    private Date updateTime;
    private Integer count;
    private Long maxTime;
    private String remark;

    public ScriptEntity() {
    }

    protected ScriptEntity(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        name = in.readString();
        if (in.readByte() == 0) {
            count = null;
        } else {
            count = in.readInt();
        }
        if (in.readByte() == 0) {
            maxTime = null;
        } else {
            maxTime = in.readLong();
        }
        if (in.readByte() == 0) {
            createTime = null;
        } else {
            createTime = new Date(in.readLong());
        }
        if (in.readByte() == 0) {
            updateTime = null;
        } else {
            updateTime = new Date(in.readLong());
        }
        remark = in.readString();
    }

    public static final Creator<ScriptEntity> CREATOR = new Creator<>() {
        @Override
        public ScriptEntity createFromParcel(Parcel in) {
            return new ScriptEntity(in);
        }

        @Override
        public ScriptEntity[] newArray(int size) {
            return new ScriptEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        dest.writeString(name);
        if (count == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(count);
        }
        if (maxTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(maxTime);
        }
        if (createTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(createTime.getTime());
        }
        if (updateTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(updateTime.getTime());
        }
        dest.writeString(remark);
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
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
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
        notifyPropertyChanged(BR.count);
    }

    @Bindable
    public Long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Long maxTime) {
        this.maxTime = maxTime;
        notifyPropertyChanged(BR.maxTime);
    }

    @Bindable
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
        notifyPropertyChanged(BR.remark);
    }
}
