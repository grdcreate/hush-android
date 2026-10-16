package com.aurora.hush;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Pastel aurora wash, painted with layered radial gradients. */
public class AuroraView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader base;
    private int w, h;

    public AuroraView(Context context) {
        super(context);
    }

    public AuroraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;
        base = new LinearGradient(0, 0, w, h,
                new int[]{0xFFFFC79A, 0xFFFBD3A6, 0xFFF3B8D4, 0xFFB7A8F0, 0xFF9FD9E8},
                null, Shader.TileMode.CLAMP);
    }

    private void blob(Canvas c, float cx, float cy, float r, int color) {
        paint.setShader(new RadialGradient(cx, cy, r, color, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (w == 0 || h == 0) return;
        paint.setShader(base);
        canvas.drawRect(0, 0, w, h, paint);
        blob(canvas, w * 0.12f, h * 0.22f, w * 0.55f, 0xCCFF8A5C);
        blob(canvas, w * 0.48f, h * 0.06f, w * 0.45f, 0xB3FFD66B);
        blob(canvas, w * 0.88f, h * 0.26f, w * 0.50f, 0xB3C79BFF);
        blob(canvas, w * 0.28f, h * 0.72f, w * 0.55f, 0x99FF9EBB);
        blob(canvas, w * 0.78f, h * 0.80f, w * 0.50f, 0x998FD3FF);
        blob(canvas, w * 0.55f, h * 0.48f, w * 0.42f, 0x807FE0C3);
        blob(canvas, w * 0.05f, h * 0.55f, w * 0.35f, 0x66FFF3B0);
        paint.setShader(null);
        canvas.drawColor(Color.TRANSPARENT);
    }
}
