package pub.carzy.auto_script.db.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.KeyEvent;

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
    /**
     * 主键
     */
    @PrimaryKey
    private Long id;
    /**
     * 父级主键
     */
    @ColumnInfo(name = "script_id")
    private Long scriptId;
    /**
     * 手势索引
     */
    private Integer index = 0;
    /**
     * 按下时间
     */
    @ColumnInfo(name = "start_time")
    private Long startTime = 0L;
    /**
     * 持续时长
     */
    @ColumnInfo(name = "duration")
    private Long duration = 0L;
    /**
     * 移动点数量
     */
    private Integer pointCount = 0;
    /**
     * 类型,1是手势,2是按键
     */
    private Integer type = 1;
    /**
     * 键
     */
    private Integer code = KeyEvent.KEYCODE_HOME;
    private String description = "";

    public ScriptActionEntity() {
    }

    protected ScriptActionEntity(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        if (in.readByte() == 0) {
            scriptId = null;
        } else {
            scriptId = in.readLong();
        }
        if (in.readByte() == 0) {
            index = null;
        } else {
            index = in.readInt();
        }
        if (in.readByte() == 0) {
            startTime = null;
        } else {
            startTime = in.readLong();
        }
        if (in.readByte() == 0) {
            duration = null;
        } else {
            duration = in.readLong();
        }
        if (in.readByte() == 0) {
            pointCount = null;
        } else {
            pointCount = in.readInt();
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
        description = in.readString();
    }

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
        if (scriptId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(scriptId);
        }
        if (index == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(index);
        }
        if (startTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(startTime);
        }
        if (duration == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(duration);
        }
        if (pointCount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(pointCount);
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
    public Long getScriptId() {
        return scriptId;
    }

    public void setScriptId(Long scriptId) {
        this.scriptId = scriptId;
        notifyPropertyChanged(BR.scriptId);
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
    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
        notifyPropertyChanged(BR.startTime);
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
    public Integer getPointCount() {
        return pointCount;
    }

    public void setPointCount(Integer pointCount) {
        this.pointCount = pointCount;
        notifyPropertyChanged(BR.count);
    }

    @Bindable
    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
        notifyPropertyChanged(BR.duration);
    }

    @Bindable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        notifyPropertyChanged(BR.description);
    }

    @Bindable
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
        notifyPropertyChanged(BR.type);
    }
}
