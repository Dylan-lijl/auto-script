package pub.carzy.auto_script.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 普通分割装饰器
 * @author admin
 */
public class NormalDividerItemDecoration extends DividerItemDecoration {

    /**
     * Creates a divider {@link RecyclerView.ItemDecoration} that can be used with a
     *
     * @param context     Current context, it will be used to access resources.
     * @param orientation Divider orientation. Should be {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public NormalDividerItemDecoration(Context context, int orientation) {
        super(context, orientation);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        // 关键点：childCount - 1，强制跳过最后一项
        int childCount = parent.getChildCount();
        Drawable drawable = getDrawable();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int top = child.getBottom() + params.bottomMargin;
            assert drawable != null;
            int bottom = top + drawable.getIntrinsicHeight();
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(c);
        }
    }
}
