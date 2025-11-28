package pub.carzy.auto_script.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class ScriptPointEntity extends BaseObservable implements Parcelable {
    private Long id;
    private Long parentId;
    private Float x;
    private Float y;
    private Long time;
    private Integer toolType;

    public ScriptPointEntity() {
    }
    protected ScriptPointEntity(Parcel in) {
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
            x = null;
        } else {
            x = in.readFloat();
        }
        if (in.readByte() == 0) {
            y = null;
        } else {
            y = in.readFloat();
        }
        if (in.readByte() == 0) {
            time = null;
        } else {
            time = in.readLong();
        }
        if (in.readByte() == 0) {
            toolType = null;
        } else {
            toolType = in.readInt();
        }
    }

    public static final Creator<ScriptPointEntity> CREATOR = new Creator<>() {
        @Override
        public ScriptPointEntity createFromParcel(Parcel in) {
            return new ScriptPointEntity(in);
        }

        @Override
        public ScriptPointEntity[] newArray(int size) {
            return new ScriptPointEntity[size];
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
        if (x == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(x);
        }
        if (y == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(y);
        }
        if (time == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(time);
        }
        if (toolType == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(toolType);
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
    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
        notifyPropertyChanged(BR.x);
    }

    @Bindable
    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
        notifyPropertyChanged(BR.y);
    }

    @Bindable
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
        notifyPropertyChanged(BR.time);
    }

    @Bindable
    public Integer getToolType() {
        return toolType;
    }

    public void setToolType(Integer toolType) {
        this.toolType = toolType;
        notifyPropertyChanged(BR.toolType);
    }
}
