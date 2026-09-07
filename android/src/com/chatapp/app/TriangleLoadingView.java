package com.chatapp.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** 自定义加载动画: 深灰框架, 边按顺序(左->下->右)累积点亮, 全亮后一起熄灭循环 */
public class TriangleLoadingView extends View {

    private final Paint darkPaint = new Paint();
    private final Paint accentPaint = new Paint();
    private float phase = 0f;
    private ValueAnimator animator;

    public TriangleLoadingView(Context c) {
        super(c);
        darkPaint.setStyle(Paint.Style.STROKE);
        darkPaint.setStrokeWidth(dp(4));
        darkPaint.setStrokeCap(Paint.Cap.ROUND);
        darkPaint.setColor(0xFF3A3A3C);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(dp(5));
        accentPaint.setStrokeCap(Paint.Cap.ROUND);
        accentPaint.setColor(Utils.ACCENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int s = dp(56);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 4f);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                phase = (float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        accentPaint.setColor(Utils.ACCENT); // 黑夜绿 / 白昼蓝
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) * 0.32f;
        float ax = cx, ay = cy - r;
        float bx = cx - r * 0.866f, by = cy + r * 0.5f;
        float dx = cx + r * 0.866f, dy = cy + r * 0.5f;
        float[][] edges = {
                {ax, ay, bx, by}, // 左边
                {bx, by, dx, dy}, // 下边
                {dx, dy, ax, ay}  // 右边
        };
        // 先画全部灰色框架, 再画点亮边(绿色永远在上层, 避免灰色覆盖)
        for (float[] e : edges) {
            canvas.drawLine(e[0], e[1], e[2], e[3], darkPaint);
        }
        int lit = Math.min(3, (int) phase);
        for (int i = 0; i < lit && i < 3; i++) {
            float[] e = edges[i];
            canvas.drawLine(e[0], e[1], e[2], e[3], accentPaint);
        }
    }

    private int dp(float v) {
        return (int) (getResources().getDisplayMetrics().density * v + 0.5f);
    }
}
