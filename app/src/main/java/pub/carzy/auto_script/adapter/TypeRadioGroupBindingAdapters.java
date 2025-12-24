package pub.carzy.auto_script.adapter;

import android.widget.RadioGroup;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.db.entity.ScriptActionEntity;

/**
 * action type绑定
 * @author admin
 */
public class TypeRadioGroupBindingAdapters {
    @BindingAdapter("actionType")
    public static void setActionType(RadioGroup group, int type) {
        int checkedId =
                type == ScriptActionEntity.GESTURE
                        ? R.id.type_gesture
                        : R.id.type_key_event;

        if (group.getCheckedRadioButtonId() != checkedId) {
            group.check(checkedId);
        }
    }

    @InverseBindingAdapter(attribute = "actionType")
    public static int getActionType(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        return id == R.id.type_gesture
                ? ScriptActionEntity.GESTURE
                : ScriptActionEntity.KEY_EVENT;
    }

    @BindingAdapter(value = "actionTypeAttrChanged")
    public static void setActionTypeListener(
            RadioGroup group,
            final InverseBindingListener listener
    ) {
        if (listener == null) return;
        group.setOnCheckedChangeListener(
                (g, checkedId) -> listener.onChange()
        );
    }
}
