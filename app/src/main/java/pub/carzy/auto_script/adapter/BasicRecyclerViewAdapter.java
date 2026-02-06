package pub.carzy.auto_script.adapter;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.RecyclerView;


/**
 * @author admin
 */
public abstract class BasicRecyclerViewAdapter<V extends RecyclerView.ViewHolder, D>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected final ObservableList<D> data = new ObservableArrayList<>();

    private static final int TYPE_EMPTY = -1;
    private static final int TYPE_NORMAL = 0;

    public BasicRecyclerViewAdapter() {
        data.addOnListChangedCallback(new ObservableList.OnListChangedCallback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChanged(ObservableList<D> sender) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeInserted(ObservableList<D> sender, int positionStart, int itemCount) {
                notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeRemoved(ObservableList<D> sender, int positionStart, int itemCount) {
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onItemRangeMoved(ObservableList<D> sender, int fromPosition, int toPosition, int itemCount) {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(ObservableList<D> sender, int positionStart, int itemCount) {
                notifyItemRangeChanged(positionStart, itemCount);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return data.isEmpty() ? TYPE_EMPTY : TYPE_NORMAL;
    }

    @Override
    public int getItemCount() {
        return data.isEmpty() ? 1 : data.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_EMPTY) {
            return onCreateEmptyViewHolder(parent);
        }
        return onCreateNormalViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_EMPTY) {
            onBindEmpty(holder);
            return;
        }

        onBindNormal((V) holder, data.get(position), position);
    }

    protected abstract V onCreateNormalViewHolder(ViewGroup parent);

    protected abstract void onBindNormal(V holder, D item, int position);

    protected RecyclerView.ViewHolder onCreateEmptyViewHolder(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new RecyclerView.ViewHolder(view) {
        };
    }

    protected void onBindEmpty(RecyclerView.ViewHolder holder) {
    }

    public ObservableList<D> getData() {
        return data;
    }
}
