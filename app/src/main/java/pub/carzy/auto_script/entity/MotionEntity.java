package pub.carzy.auto_script.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author admin
 */
@Data
public class MotionEntity implements Parcelable {
    private Integer index;
    private Long downTime;
    private List<PointEntity> points = new ArrayList<>();

    public MotionEntity() {
    }
    protected MotionEntity(Parcel in) {
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
        points = in.createTypedArrayList(PointEntity.CREATOR);
    }

    public static final Creator<MotionEntity> CREATOR = new Creator<MotionEntity>() {
        @Override
        public MotionEntity createFromParcel(Parcel in) {
            return new MotionEntity(in);
        }

        @Override
        public MotionEntity[] newArray(int size) {
            return new MotionEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
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
        dest.writeTypedList(points);
    }
}
