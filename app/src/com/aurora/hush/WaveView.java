package com.aurora.hush;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Soft wave divider that melts the aurora header into the dark body. */
public class WaveView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public WaveView(Context context) {
        super(context);
        paint.setColor(0xFF0B0C0E);
    }

    public WaveView(Context context, AttributeSet attrs) {
        this(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        path.reset();
        path.moveTo(0, h * 0.42f);
        path.cubicTo(w * 0.22f, h * 0.02f, w * 0.42f, h * 0.98f, w * 0.68f, h * 0.55f);
        path.cubicTo(w * 0.82f, h * 0.32f, w * 0.92f, h * 0.30f, w, h * 0.44f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        canvas.drawPath(path, paint);
    }
}
