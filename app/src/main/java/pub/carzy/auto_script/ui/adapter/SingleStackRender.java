package pub.carzy.auto_script.ui.adapter;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.buffer.HorizontalBarBuffer;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.List;

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
            if (vals == null || vals.length < 2) continue;
            float start = vals[0];
            for (int k = 1; k < vals.length; k++) {
                float end = start + vals[k];

                buffer.buffer[bufferIndex++] = start;
                buffer.buffer[bufferIndex++] = entry.getX() - barWidthHalf;
                buffer.buffer[bufferIndex++] = end;
                buffer.buffer[bufferIndex++] = entry.getX() + barWidthHalf;

                start = end;
            }
        }
    }

    @Override
    public void drawValues(Canvas c) {
        if (!isDrawingValuesAllowed(mChart)) return;

        List<IBarDataSet> dataSets = mChart.getBarData().getDataSets();
        final float valueOffsetPlus = Utils.convertDpToPixel(5f);
        final boolean drawValueAboveBar = mChart.isDrawValueAboveBarEnabled();
        final float halfTextHeight = Utils.calcTextHeight(mValuePaint, "10") / 2f;

        for (int i = 0; i < mChart.getBarData().getDataSetCount(); i++) {
            IBarDataSet dataSet = dataSets.get(i);
            if (!shouldDrawValues(dataSet)) continue;

            applyValueTextStyle(dataSet);
            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
            BarBuffer buffer = mBarBuffers[i];

            MPPointF iconsOffset = MPPointF.getInstance(dataSet.getIconsOffset());
            iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x);
            iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y);

            for (int j = 0; j < dataSet.getEntryCount(); j++) {
                BarEntry entry = dataSet.getEntryForIndex(j);
                float[] vals = entry.getYVals();
                if (vals == null || vals.length < 2) continue;

                // 找到最后一段在 buffer 中的索引
                int bufferIndex = j * 4 * (vals.length - 1) + (vals.length - 2) * 4;

                float left = buffer.buffer[bufferIndex];
                float top = buffer.buffer[bufferIndex + 1];
                float right = buffer.buffer[bufferIndex + 2];
                float bottom = buffer.buffer[bufferIndex + 3];

                float y = (top + bottom) / 2f;

                // 绘制数值
                String formattedValue = dataSet.getValueFormatter().getBarLabel(entry);
                float valueTextWidth = Utils.calcTextWidth(mValuePaint, formattedValue);
                float xOffset = drawValueAboveBar ? valueOffsetPlus : -(valueTextWidth + valueOffsetPlus);
                if (mChart.isInverted(dataSet.getAxisDependency())) {
                    xOffset = -xOffset - valueTextWidth;
                }
                float x = right + xOffset;

                if (dataSet.isDrawValuesEnabled()) {
                    drawValue(c, formattedValue, x, y + halfTextHeight, dataSet.getValueTextColor(j));
                }

                // 绘制图标
                if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {
                    float px = x + iconsOffset.x;
                    float py = y + iconsOffset.y;
                    Utils.drawImage(c, entry.getIcon(),
                            (int) px, (int) py,
                            entry.getIcon().getIntrinsicWidth(),
                            entry.getIcon().getIntrinsicHeight());
                }
            }

            MPPointF.recycleInstance(iconsOffset);
        }
    }

}
