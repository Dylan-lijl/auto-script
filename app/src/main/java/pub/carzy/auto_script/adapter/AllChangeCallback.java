package pub.carzy.auto_script.adapter;

import androidx.databinding.ObservableList;

/**
 * @author admin
 */
public abstract class AllChangeCallback<T extends ObservableList> extends ObservableList.OnListChangedCallback<T> {
    @Override
    public void onItemRangeChanged(T sender, int positionStart, int itemCount) {
        onChanged(sender);
    }

    @Override
    public void onItemRangeInserted(T sender, int positionStart, int itemCount) {
        onChanged(sender);
    }

    @Override
    public void onItemRangeMoved(T sender, int fromPosition, int toPosition, int itemCount) {
        onChanged(sender);
    }

    @Override
    public void onItemRangeRemoved(T sender, int positionStart, int itemCount) {
        onChanged(sender);
    }
}
