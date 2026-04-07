package pub.carzy.auto_script.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * 绘制网格层
 *
 * @author admin
 */
public class GridDrawable extends Drawable {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int size;
    private boolean grid;
    private int lineWidth;
    private int color;
    private int gridColor;

    public void setConfig(Integer color, Integer size, Boolean grid, Integer lineWidth, Integer gridColor) {
        this.color = color;
        this.size = size;
        this.grid = grid;
        this.lineWidth = lineWidth;
        this.gridColor = gridColor;

        // 初始化线条画笔
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setColor(gridColor);

        // 初始化文字画笔
        textPaint.setColor(linePaint.getColor());
        textPaint.setTextSize(30f);
        textPaint.setFakeBoldText(false);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (size<=0){
            return;
        }
        Rect bounds = getBounds();
        float width = bounds.width();
        float height = bounds.height();

        int halfSize = size;
        float centerX = bounds.left + width / 2f;
        float centerY = bounds.top + height / 2f;

        float stepX = width / (float) (halfSize * 2);
        float stepY = height / (float) (halfSize * 2);

        // 1. 计算文字度量信息
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float vOffset = (textHeight / 2) - fm.descent;
        float padding = 6f;

        // 2. 动态计算步进（防止文字粘连）
        // 测量一个典型的 4 位数坐标宽度，加上安全间距
        float sampleWidth = textPaint.measureText("1080");
        int intervalX = (int) Math.ceil((sampleWidth + 50f) / stepX);
        intervalX = Math.max(1, intervalX);

        int intervalY = (int) Math.ceil((textHeight + 40f) / stepY);
        intervalY = Math.max(1, intervalY);

        // --- 开始绘制 ---

        // 3. 绘制竖线与 X 坐标
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = -halfSize; i <= halfSize; i++) {
            float x = centerX + i * stepX;
            if (grid){
                // 先画完整的线
                canvas.drawLine(x, bounds.top, x, bounds.bottom, linePaint);
            }

            // 绘制坐标文字（带遮罩）
            if (i % intervalX == 0 && x > bounds.left + 80 && x < bounds.right - 80) {
                String label = String.valueOf(Math.round(x - bounds.left));
                float tw = textPaint.measureText(label);

                // 绘制遮罩矩形 (断线效果)
                float labelY = bounds.top + textHeight + 20;
                // 绘制文字
                canvas.drawText(label, x, labelY, textPaint);
            }
        }

        // 4. 绘制横线与 Y 坐标
        textPaint.setTextAlign(Paint.Align.LEFT);
        for (int i = -halfSize; i <= halfSize; i++) {
            float y = centerY + i * stepY;
            if (grid){
                canvas.drawLine(bounds.left, y, bounds.right, y, linePaint);
            }

            // 绘制坐标文字（带遮罩）
            if (i % intervalY == 0 && y > bounds.top + 80 && y < bounds.bottom - 80) {
                String label = String.valueOf(Math.round(y - bounds.top));
                float tw = textPaint.measureText(label);

                // 绘制遮罩矩形 (断线效果)
                float labelX = bounds.left + 20;
                // 绘制文字：y + vOffset 确保线从文字中间穿过
                canvas.drawText(label, labelX, y + vOffset, textPaint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
