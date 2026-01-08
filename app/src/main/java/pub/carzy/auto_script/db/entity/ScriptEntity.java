package pub.carzy.auto_script.db.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

import lombok.EqualsAndHashCode;
import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
@Entity(tableName = "script")
@EqualsAndHashCode(callSuper = false)
public class ScriptEntity extends BaseObservable implements Parcelable {
    @PrimaryKey
    private Long id;
    private String name;
    @ColumnInfo(name = "create_time")
    private Date createTime;
    @ColumnInfo(name = "update_time")
    private Date updateTime;
    private Integer actionCount;
    @ColumnInfo(name = "total_duration")
    private Long totalDuration;
    private String description;

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
            actionCount = null;
        } else {
            actionCount = in.readInt();
        }
        if (in.readByte() == 0) {
            totalDuration = null;
        } else {
            totalDuration = in.readLong();
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
        description = in.readString();
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
        if (actionCount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(actionCount);
        }
        if (totalDuration == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(totalDuration);
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
        dest.writeString(description);
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
    public Integer getActionCount() {
        return actionCount;
    }

    public void setActionCount(Integer actionCount) {
        this.actionCount = actionCount;
        notifyPropertyChanged(BR.count);
    }

    @Bindable
    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
        notifyPropertyChanged(BR.totalDuration);
    }

    @Bindable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        notifyPropertyChanged(BR.description);
    }
}
