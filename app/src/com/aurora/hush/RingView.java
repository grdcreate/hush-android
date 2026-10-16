package com.aurora.hush;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/** Circular progress ring with a centered glyph — the signature element of the UI. */
public class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Drawable icon;
    private float progress = 0f;
    private int color = 0xFF2EE6A8;
    private float strokePx;

    public RingView(Context context) {
        super(context);
        init();
    }

    public RingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePx = getResources().getDisplayMetrics().density * 5f;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(strokePx);
        track.setColor(0xFF2A2C31);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(strokePx);
        arc.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float p) {
        this.progress = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    public void setColor(int c) {
        this.color = c;
        invalidate();
    }

    public void setIcon(Drawable d) {
        this.icon = d;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float pad = strokePx / 2f + 1f;
        rect.set(pad, pad, w - pad, h - pad);
        canvas.drawOval(rect, track);
        if (progress > 0.005f) {
            arc.setColor(color);
            canvas.drawArc(rect, -90f, progress * 360f, false, arc);
        }
        if (icon != null) {
            int size = (int) (Math.min(w, h) * 0.44f);
            int cx = w / 2;
            int cy = h / 2;
            icon.setBounds(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
            icon.draw(canvas);
        }
    }
}
