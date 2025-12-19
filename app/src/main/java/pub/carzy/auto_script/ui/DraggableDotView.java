package pub.carzy.auto_script.ui;

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

    private final int r;
    private float lastRawX;
    private float lastRawY;

    private OnDotMoveListener moveListener;

    public DraggableDotView(Context context, int radiusPx) {
        super(context);
        this.r = radiusPx;

        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        setClickable(true);
    }

    public void setOnDotMoveListener(OnDotMoveListener listener) {
        this.moveListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(r * 2, r * 2);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 填充
        canvas.drawCircle(r, r, r - 1, fillPaint);
        // 边框
        canvas.drawCircle(r, r, r - 1, strokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 按下 → 蓝色
                fillPaint.setColor(Color.BLUE);
                invalidate();
                lastRawX = event.getRawX();
                lastRawY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float rawX = event.getRawX();
                float rawY = event.getRawY();

                float dx = rawX - lastRawX;
                float dy = rawY - lastRawY;

                // 移动 View（关键）
                setX(getX() + dx);
                setY(getY() + dy);

                lastRawX = rawX;
                lastRawY = rawY;

                if (moveListener != null) {
                    // 返回的是“圆心坐标”
                    moveListener.onMove(
                            getX() + r,
                            getY() + r
                    );
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 抬起 → 恢复白色
                fillPaint.setColor(Color.WHITE);
                invalidate();

                if (moveListener != null) {
                    moveListener.onUp(
                            getX() + r,
                            getY() + r
                    );
                }
                return true;
        }
        return super.onTouchEvent(event);
    }
    public static interface OnDotMoveListener {
        void onMove(float x, float y);
        void onUp(float x, float y);
    }
}

