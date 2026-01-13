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
import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Entity(tableName = "script_point")
public class ScriptPointEntity extends BaseObservable implements Parcelable {
    @PrimaryKey
    private Long id;
    @ColumnInfo(name = "script_id")
    private Long scriptId;
    @ColumnInfo(name = "action_id")
    private Long actionId;
    @ColumnInfo(name = "x")
    private Float x;
    @ColumnInfo(name = "y")
    private Float y;
    @ColumnInfo(name = "delta_time")
    private Long deltaTime;
    @ColumnInfo(name = "order")
    public Float order;
    @ColumnInfo(name = "description")
    public String description = "";

    public ScriptPointEntity() {
    }

    protected ScriptPointEntity(Parcel in) {
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
            actionId = null;
        } else {
            actionId = in.readLong();
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
            deltaTime = null;
        } else {
            deltaTime = in.readLong();
        }
        if (in.readByte() == 0) {
            order = null;
        } else {
            order = in.readFloat();
        }
        description = in.readString();
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
        if (scriptId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(scriptId);
        }
        if (actionId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(actionId);
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
        if (deltaTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(deltaTime);
        }
        if (order == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(order);
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
    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
        notifyPropertyChanged(BR.actionId);
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
    public Long getDeltaTime() {
        return deltaTime;
    }

    public void setDeltaTime(Long deltaTime) {
        this.deltaTime = deltaTime;
        notifyPropertyChanged(BR.deltaTime);
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
    public Float getOrder() {
        return order;
    }

    public void setOrder(Float order) {
        this.order = order;
        notifyPropertyChanged(BR.order);
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
