package com.khang.attendanceapp.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.khang.attendanceapp.R;

/**
 * Lớp phủ khung quét: làm tối vùng ngoài khung, vẽ 4 góc và tia quét chạy lên xuống.
 */
public class ScanOverlayView extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dimPath = new Path();
    private final RectF frame = new RectF();

    private float lineProgress = 0f;
    private boolean lineVisible = true;
    private ValueAnimator animator;

    public ScanOverlayView(Context context) {
        this(context, null);
    }

    public ScanOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        dimPaint.setColor(ContextCompat.getColor(context, R.color.scrim_dark));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(5 * d);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setColor(ContextCompat.getColor(context, R.color.scan_idle));
        linePaint.setStrokeWidth(3 * d);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(ContextCompat.getColor(context, R.color.scan_success));
    }

    /** Đổi màu 4 góc (trắng / xanh khi quét được / vàng khi trùng). */
    public void setFrameColor(@ColorInt int color) {
        cornerPaint.setColor(color);
        invalidate();
    }

    public void setLineVisible(boolean visible) {
        lineVisible = visible;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            lineProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float fw = w * 0.84f;
        float fh = fw * 0.56f;
        float cx = w / 2f;
        float cy = h * 0.40f;
        frame.set(cx - fw / 2f, cy - fh / 2f, cx + fw / 2f, cy + fh / 2f);

        float d = getResources().getDisplayMetrics().density;
        dimPath.reset();
        dimPath.setFillType(Path.FillType.EVEN_ODD);
        dimPath.addRect(0, 0, w, h, Path.Direction.CW);
        dimPath.addRoundRect(frame, 20 * d, 20 * d, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;

        canvas.drawPath(dimPath, dimPaint);

        // 4 góc
        float len = 34 * d;
        float r = 20 * d;
        float l = frame.left, t = frame.top, rr = frame.right, b = frame.bottom;
        Path p = new Path();
        p.moveTo(l, t + len); p.lineTo(l, t + r); p.quadTo(l, t, l + r, t); p.lineTo(l + len, t);
        p.moveTo(rr - len, t); p.lineTo(rr - r, t); p.quadTo(rr, t, rr, t + r); p.lineTo(rr, t + len);
        p.moveTo(rr, b - len); p.lineTo(rr, b - r); p.quadTo(rr, b, rr - r, b); p.lineTo(rr - len, b);
        p.moveTo(l + len, b); p.lineTo(l + r, b); p.quadTo(l, b, l, b - r); p.lineTo(l, b - len);
        canvas.drawPath(p, cornerPaint);

        // tia quét
        if (lineVisible) {
            float y = frame.top + 12 * d + (frame.height() - 24 * d) * lineProgress;
            linePaint.setAlpha(220);
            canvas.drawLine(frame.left + 14 * d, y, frame.right - 14 * d, y, linePaint);
            linePaint.setAlpha(60);
            canvas.drawRect(frame.left + 14 * d, y - 10 * d, frame.right - 14 * d, y, linePaint);
        }
    }
}
