package pub.carzy.auto_script.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class PointEntity implements Parcelable {
    private Float x;
    private Float y;
    private Long time;
    private Integer toolType;

    public PointEntity() {
    }

    public PointEntity(Float x, Float y, Long time, Integer toolType) {
        this.x = x;
        this.y = y;
        this.time = time;
        this.toolType = toolType;
    }

    protected PointEntity(Parcel in) {
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

    public static final Creator<PointEntity> CREATOR = new Creator<>() {
        @Override
        public PointEntity createFromParcel(Parcel in) {
            return new PointEntity(in);
        }

        @Override
        public PointEntity[] newArray(int size) {
            return new PointEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
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
}
