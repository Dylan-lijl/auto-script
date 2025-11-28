package pub.carzy.auto_script.db.view;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import pub.carzy.auto_script.db.ScriptActionEntity;
import pub.carzy.auto_script.db.ScriptEntity;
import pub.carzy.auto_script.db.ScriptPointEntity;

/**
 * @author admin
 */
@Data
public class ScriptVoEntity implements Parcelable {

    // 根实体 (单个对象)
    private ScriptEntity root;

    private List<ScriptActionEntity> actions = new ArrayList<>();

    // 子实体列表 (List)
    private List<ScriptPointEntity> points = new ArrayList<>();

    public ScriptVoEntity() {
    }

    protected ScriptVoEntity(Parcel in) {
        // root 依然可以直接 readParcelable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            root = in.readParcelable(ScriptEntity.class.getClassLoader(), ScriptEntity.class);
        } else {
            root = in.readParcelable(ScriptEntity.class.getClassLoader());
        }
        // 读入列表到已初始化的列表中
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            in.readTypedList(actions, ScriptActionEntity.CREATOR);
            in.readTypedList(points, ScriptPointEntity.CREATOR);
        } else {
            in.readTypedList(actions, ScriptActionEntity.CREATOR);
            in.readTypedList(points, ScriptPointEntity.CREATOR);
        }
    }


    public static final Creator<ScriptVoEntity> CREATOR = new Creator<ScriptVoEntity>() {
        @Override
        public ScriptVoEntity createFromParcel(Parcel in) {
            return new ScriptVoEntity(in);
        }

        @Override
        public ScriptVoEntity[] newArray(int size) {
            return new ScriptVoEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(root, flags);
        dest.writeTypedList(actions);
        dest.writeTypedList(points);
    }
}
