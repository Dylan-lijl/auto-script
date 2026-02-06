package pub.carzy.auto_script.adapter;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qmuiteam.qmui.recyclerView.QMUIRVItemSwipeAction;
import com.qmuiteam.qmui.recyclerView.QMUISwipeViewHolder;

/**
 * @author admin
 */
public abstract class BasicSwipeActionCallback extends QMUIRVItemSwipeAction.Callback {
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, boolean isCurrentlyActive, int swipeDirection) {
        if (viewHolder instanceof QMUISwipeViewHolder) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, isCurrentlyActive, swipeDirection);
        }
    }
}
