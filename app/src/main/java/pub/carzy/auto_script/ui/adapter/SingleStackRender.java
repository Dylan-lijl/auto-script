package pub.carzy.auto_script.ui.adapter;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.buffer.HorizontalBarBuffer;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * @author admin
 */
public class SingleStackRender extends HorizontalBarChartRenderer {

    private RectF mBarShadowRectBuffer = new RectF();

    public SingleStackRender(BarDataProvider chart, ChartAnimator animator,
                             ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    @Override
    public void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
        BarBuffer buffer = mBarBuffers[index];

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        // 初始化 buffer
        buffer.setPhases(phaseX, phaseY);
        buffer.setDataSet(index);
        buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
        buffer.setBarWidth(mChart.getBarData().getBarWidth());

        // 自定义 buffer 只绘制 n-1 段
        feedSingleStackBuffer(buffer, dataSet);

        // 转换为像素
        trans.pointValuesToPixel(buffer.buffer);

        // 绘制柱体
        final boolean isSingleColor = dataSet.getColors().size() == 1;
        float barWidthHalf = mChart.getBarData().getBarWidth() / 2f;

        for (int j = 0; j < buffer.size(); j += 4) {

            if (!mViewPortHandler.isInBoundsTop(buffer.buffer[j + 3]))
                break;
            if (!mViewPortHandler.isInBoundsBottom(buffer.buffer[j + 1]))
                continue;

            // 设置颜色
            if (isSingleColor) {
                mRenderPaint.setColor(dataSet.getColor());
            } else {
                mRenderPaint.setColor(dataSet.getColor(j / 4));
            }

            // 绘制矩形
            c.drawRect(buffer.buffer[j], buffer.buffer[j + 1],
                    buffer.buffer[j + 2], buffer.buffer[j + 3],
                    mRenderPaint);

            // 绘制边框
            if (dataSet.getBarBorderWidth() > 0f) {
                mBarBorderPaint.setColor(dataSet.getBarBorderColor());
                mBarBorderPaint.setStrokeWidth(Utils.convertDpToPixel(dataSet.getBarBorderWidth()));
                c.drawRect(buffer.buffer[j], buffer.buffer[j + 1],
                        buffer.buffer[j + 2], buffer.buffer[j + 3],
                        mBarBorderPaint);
            }

            // 绘制阴影（可选）
            if (mChart.isDrawBarShadowEnabled()) {
                mBarShadowRectBuffer.top = buffer.buffer[j + 1];
                mBarShadowRectBuffer.bottom = buffer.buffer[j + 3];
                mBarShadowRectBuffer.left = 0f;
                mBarShadowRectBuffer.right = buffer.buffer[j + 2];
                c.drawRect(mBarShadowRectBuffer, mShadowPaint);
            }
        }
    }

    /**
     * 自定义 buffer 只绘制 n-1 段堆叠柱
     */
    private void feedSingleStackBuffer(BarBuffer buffer, IBarDataSet dataSet) {
        int bufferIndex = 0;
        float barWidth = mChart.getBarData().getBarWidth();
        float barWidthHalf = barWidth / 2f;

        for (int i = 0; i < dataSet.getEntryCount(); i++) {
            BarEntry entry = dataSet.getEntryForIndex(i);
            float[] vals = entry.getYVals();

            if (vals == null || vals.length < 2) continue; // 非堆叠或长度小于2跳过

            float start = vals[0]; // 从第一段结束开始
            for (int k = 1; k < vals.length; k++) {
                float end = start + vals[k];

                buffer.buffer[bufferIndex++] = start;                  // left
                buffer.buffer[bufferIndex++] = entry.getX() - barWidthHalf; // top
                buffer.buffer[bufferIndex++] = end;                    // right
                buffer.buffer[bufferIndex++] = entry.getX() + barWidthHalf; // bottom

                start = end;
            }
        }
    }
}
