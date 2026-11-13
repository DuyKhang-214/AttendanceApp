package com.khang.attendanceapp.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.khang.attendanceapp.R;

/** Biểu đồ cột tự vẽ bằng Canvas (số lượt điểm danh 7 ngày gần nhất). */
public class BarChartView extends View {

    private int[] values = new int[0];
    private String[] labels = new String[0];
    private float progress = 1f;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final int colorBar;
    private final int colorToday;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        colorBar = ContextCompat.getColor(context, R.color.primary);
        colorToday = ContextCompat.getColor(context, R.color.accent);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.primary_light));
        float d = getResources().getDisplayMetrics().density;
        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextSize(12 * d);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        valuePaint.setTextSize(12 * d);
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(700);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        if (n == 0) return;

        float d = getResources().getDisplayMetrics().density;
        float labelH = 22 * d;
        float topPad = 20 * d;
        float chartH = getHeight() - labelH - topPad;
        float slot = (float) getWidth() / n;
        float barW = Math.min(slot * 0.5f, 32 * d);
        float radius = 8 * d;

        int max = 1;
        for (int v : values) max = Math.max(max, v);

        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            float left = cx - barW / 2f;
            float right = cx + barW / 2f;
            float bottom = topPad + chartH;

            // nền cột
            rect.set(left, topPad, right, bottom);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            // cột giá trị
            float h = chartH * ((float) values[i] / max) * progress;
            if (values[i] > 0) h = Math.max(h, radius * 1.2f);
            barPaint.setColor(i == n - 1 ? colorToday : colorBar);
            rect.set(left, bottom - h, right, bottom);
            canvas.drawRoundRect(rect, radius, radius, barPaint);

            if (values[i] > 0) {
                canvas.drawText(String.valueOf(values[i]), cx, bottom - h - 6 * d, valuePaint);
            }
            if (i < labels.length) {
                canvas.drawText(labels[i], cx, getHeight() - 4 * d, labelPaint);
            }
        }
    }
}
