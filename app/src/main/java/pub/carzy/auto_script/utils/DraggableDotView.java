package pub.carzy.auto_script.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author admin
 */
@SuppressLint("ViewConstructor")
public class DraggableDotView extends View {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** 半径 */
    private int r;

    /** 父容器在屏幕上的位置 */
    private int parentLeft;
    private int parentTop;

    private OnDotMoveListener listener;

    public DraggableDotView(Context context, int radiusPx) {
        super(context);
        this.r = radiusPx;

        fillPaint.setColor(Color.GRAY);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);

        setClickable(true);
    }

    public void setOnDotMoveListener(OnDotMoveListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // 获取父容器在屏幕上的 raw 偏移
        View parent = (View) getParent();
        int[] loc = new int[2];
        parent.getLocationOnScreen(loc);
        parentLeft = loc[0];
        parentTop = loc[1];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(r * 2, r * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 填充圆
        canvas.drawCircle(r, r, r - 1, fillPaint);
        // 边框
        canvas.drawCircle(r, r, r - 1, strokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                fillPaint.setColor(Color.BLUE);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                float rawX = event.getRawX();
                float rawY = event.getRawY();

                // raw → view 坐标（圆心对齐）
                float newX = rawX - parentLeft - r;
                float newY = rawY - parentTop - r;

                setX(newX);
                setY(newY);

                if (listener != null) {
                    listener.onMove(rawX, rawY);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                fillPaint.setColor(Color.GRAY);
                invalidate();

                if (listener != null) {
                    listener.onUp(
                            getX() + r + parentLeft,
                            getY() + r + parentTop
                    );
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public interface OnDotMoveListener {
        /** raw 坐标（全屏） */
        void onMove(float rawX, float rawY);

        /** 抬起时 raw 坐标 */
        void onUp(float rawX, float rawY);
    }
}
