package pub.carzy.auto_script.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import pub.carzy.auto_script.databinding.MacroListTableBinding;
import pub.carzy.auto_script.db.entity.ScriptEntity;

/**
 * @author admin
 */
public class MacroTableAdapter extends ListAdapter<ScriptEntity, MacroTableAdapter.VH> {

    public MacroTableAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        MacroListTableBinding binding =
                MacroListTableBinding.inflate(inflater, parent, false);

        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VH holder,
            int position
    ) {
        ScriptEntity item = getItem(position);
        holder.binding.setItem(item);
        holder.binding.executePendingBindings();
    }

    public static class VH extends RecyclerView.ViewHolder {

        final MacroListTableBinding binding;

        VH(MacroListTableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<ScriptEntity> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ScriptEntity oldItem,
                        @NonNull ScriptEntity newItem
                ) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ScriptEntity oldItem,
                        @NonNull ScriptEntity newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };
}


