package pub.carzy.auto_script.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import pub.carzy.auto_script.databinding.ComListItemMacroListBinding;
import pub.carzy.auto_script.db.entity.ScriptEntity;

import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.RecyclerView;


import com.qmuiteam.qmui.recyclerView.QMUISwipeAction;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;


/**
 * @author admin
 */
public class MacroTableAdapter extends RecyclerView.Adapter<MacroTableAdapter.ViewHolder> {

    private final ObservableList<ScriptEntity> data;
    private QMUISwipeAction deleteAction;
    private QMUISwipeAction openAction;

    public MacroTableAdapter(Context context, ObservableList<ScriptEntity> data) {
        this.data = data;
        setHasStableIds(true);
        ObservableList.OnListChangedCallback<ObservableList<ScriptEntity>> callback = new ObservableList.OnListChangedCallback<>() {
            @Override
            public void onChanged(ObservableList<ScriptEntity> sender) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeInserted(
                    ObservableList<ScriptEntity> sender,
                    int positionStart,
                    int itemCount
            ) {
                notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeRemoved(
                    ObservableList<ScriptEntity> sender,
                    int positionStart,
                    int itemCount
            ) {
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            @Override
            public void onItemRangeChanged(
                    ObservableList<ScriptEntity> sender,
                    int positionStart,
                    int itemCount
            ) {
                notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeMoved(
                    ObservableList<ScriptEntity> sender,
                    int fromPosition,
                    int toPosition,
                    int itemCount
            ) {
                for (int i = 0; i < itemCount; i++) {
                    notifyItemMoved(fromPosition + i, toPosition + i);
                }
            }
        };
        data.addOnListChangedCallback(callback);
        QMUISwipeAction.ActionBuilder builder = new QMUISwipeAction.ActionBuilder()
                .textSize(QMUIDisplayHelper.sp2px(context, 14))
                .textColor(Color.WHITE)
                .paddingStartEnd(QMUIDisplayHelper.dp2px(context, 14));
        deleteAction = builder.text("删除").backgroundColor(Color.RED).build();
        deleteAction = builder.text("写想法").backgroundColor(Color.BLUE).build();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ComListItemMacroListBinding binding =
                ComListItemMacroListBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }


    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        ScriptEntity item = data.get(position);
        holder.bind(item);
    }


    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        final ComListItemMacroListBinding binding;

        public ViewHolder(@NonNull ComListItemMacroListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ScriptEntity item) {
            binding.setItem(item);
            binding.executePendingBindings();
        }
    }

}





