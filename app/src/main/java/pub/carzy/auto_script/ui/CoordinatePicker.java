package pub.carzy.auto_script.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.WindowManager;

import lombok.Setter;

/**
 * @author admin
 */
public class CoordinatePicker {
    /**
     * x
     */
    private float x;
    /**
     * y
     */
    private float y;
    /**
     * r
     */
    private int r;

    private final Context context;
    private final WindowManager manager;
    private DraggableDotView view;
    private boolean isAdd = false;
    @Setter
    private DraggableDotView.OnDotMoveListener listener;

    public CoordinatePicker(Context context) {
        this.context = context;
        manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setR(int r) {
        this.r = r;
    }

    public void show() {
        if (isAdd) {
            hide();
        }
        if (view==null){
            //创建一个半径为r背景颜色为白色,边框为黑色的小圆点
            view = new DraggableDotView(context, r);
            view.setOnDotMoveListener(new DraggableDotView.OnDotMoveListener() {

                @Override
                public void onMove(float x, float y) {
                    CoordinatePicker.this.x = x;
                    CoordinatePicker.this.y = y;
                    if (listener != null){
                        listener.onMove(x, y);
                    }
                }

                @Override
                public void onUp(float x, float y) {
                    CoordinatePicker.this.x = x;
                    CoordinatePicker.this.y = y;
                    if (listener != null){
                        listener.onUp(x, y);
                    }
                }
            });
        }
    }

    public void hide() {
        if (view != null) {
            manager.removeView(view);
            isAdd = false;
        }
    }
    private Drawable createDotDrawable(int r) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setSize(r * 2, r * 2);
        drawable.setColor(Color.WHITE);
        drawable.setStroke(2, Color.BLACK);
        return drawable;
    }
}
