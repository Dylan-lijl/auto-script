package pub.carzy.auto_script.db.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.R;

/**
 * @author admin
 */
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity(tableName = "script_action")
public class ScriptActionEntity extends BaseObservable implements Parcelable {
    public static final int GESTURE = 1;
    public static final int KEY_EVENT = 2;

    public static int getTypeName(int type) {
        switch (type) {
            case GESTURE:
                return R.string.gesture;
            case KEY_EVENT:
                return R.string.key_event;
            default:
                return R.string.unknown;
        }
    }

    /**
     * 主键
     */
    @PrimaryKey
    private Long id;
    /**
     * 父级主键
     */
    @ColumnInfo(name = "parent_id")
    private Long parentId;
    /**
     * 手势索引
     */
    private Integer index;
    /**
     * 序列
     */
    @ColumnInfo(name = "down_time")
    private Long downTime;
    /**
     * 按下时间
     */
    @ColumnInfo(name = "event_time")
    private Long eventTime;
    /**
     * 抬起时间
     */
    @ColumnInfo(name = "up_time")
    private Long upTime;
    /**
     * 最大的时间,一般来说这个时间是upTime,但是有的时候upTime为null,这个时候就需要遍历points
     */
    @ColumnInfo(name = "max_time")
    private Long maxTime;
    /**
     * 移动点数量
     */
    private Integer count;
    /**
     * 类型,当point小于等于2,说明是点击时间,大于2说明是滑动
     */
    private Integer type;

    private Integer code;

    public ScriptActionEntity() {
    }

    protected ScriptActionEntity(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        if (in.readByte() == 0) {
            parentId = null;
        } else {
            parentId = in.readLong();
        }
        if (in.readByte() == 0) {
            index = null;
        } else {
            index = in.readInt();
        }
        if (in.readByte() == 0) {
            downTime = null;
        } else {
            downTime = in.readLong();
        }
        if (in.readByte() == 0) {
            eventTime = null;
        } else {
            eventTime = in.readLong();
        }
        if (in.readByte() == 0) {
            upTime = null;
        } else {
            upTime = in.readLong();
        }
        if (in.readByte() == 0) {
            maxTime = null;
        } else {
            maxTime = in.readLong();
        }
        if (in.readByte() == 0) {
            count = null;
        } else {
            count = in.readInt();
        }
        if (in.readByte() == 0) {
            type = null;
        } else {
            type = in.readInt();
        }
        if (in.readByte() == 0) {
            code = null;
        } else {
            code = in.readInt();
        }
    }

    public static final Creator<ScriptActionEntity> CREATOR = new Creator<>() {
        @Override
        public ScriptActionEntity createFromParcel(Parcel in) {
            return new ScriptActionEntity(in);
        }

        @Override
        public ScriptActionEntity[] newArray(int size) {
            return new ScriptActionEntity[size];
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
        if (parentId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(parentId);
        }
        if (index == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(index);
        }
        if (downTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(downTime);
        }
        if (eventTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(eventTime);
        }
        if (upTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(upTime);
        }
        if (maxTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(maxTime);
        }
        if (count == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(count);
        }
        if (type == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(type);
        }
        if (code == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(code);
        }
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
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
        notifyPropertyChanged(BR.parentId);
    }

    @Bindable
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
        notifyPropertyChanged(BR.index);
    }

    @Bindable
    public Long getDownTime() {
        return downTime;
    }

    public void setDownTime(Long downTime) {
        this.downTime = downTime;
        notifyPropertyChanged(BR.downTime);
    }

    @Bindable
    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
        notifyPropertyChanged(BR.eventTime);
    }

    @Bindable
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
        notifyPropertyChanged(BR.code);
    }

    @Bindable
    public Long getUpTime() {
        return upTime;
    }

    public void setUpTime(Long upTime) {
        this.upTime = upTime;
        notifyPropertyChanged(BR.upTime);
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
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
        notifyPropertyChanged(BR.count);
    }

    @Bindable
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
        notifyPropertyChanged(BR.type);
    }

    public boolean checkTime() {
        return checkTime(upTime, downTime);
    }

    public static boolean checkTime(Long upTime, Long downTime) {
        if (upTime == null || downTime == null) {
            return false;
        }
        return upTime < downTime;
    }
}
