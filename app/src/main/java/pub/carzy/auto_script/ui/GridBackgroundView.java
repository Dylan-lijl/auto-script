package pub.carzy.auto_script.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * @author admin
 */
public class GridBackgroundView extends View {

    private final Paint paint;
    private final float cellSize;

    public GridBackgroundView(Context context) {
        this(context, null);
    }

    public GridBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x33000000);
        paint.setStrokeWidth(1f);
        cellSize = dp(context);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        for (float x = 0; x < w; x += cellSize) {
            canvas.drawLine(x, 0, x, h, paint);
        }

        for (float y = 0; y < h; y += cellSize) {
            canvas.drawLine(0, y, w, y, paint);
        }
    }

    private float dp(Context ctx) {
        return (float) 20 * ctx.getResources().getDisplayMetrics().density;
    }
}

