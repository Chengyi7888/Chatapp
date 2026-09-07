package com.chatapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 跳一跳小游戏：长按蓄力、松开跳跃，抛物线落点，自由落点不吸附，方块随机为三棱柱/四棱柱/六棱柱/圆柱 */
public class JumpGameActivity extends Activity {

    private GameView gameView;
    private TextView scoreView;
    private TextView bestView;
    private TextView hintView;
    private TextView overResult;
    private TextView overBest;
    private FrameLayout overPanel;
    private int best;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Theme.init(this);
        Theme.apply(this);
        best = getSharedPreferences("chatapp_jump", Context.MODE_PRIVATE).getInt("best", 0);
        buildUi();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Utils.BG);

        gameView = new GameView(this, new GameView.Listener() {
            @Override
            public void onScore(int s) {
                if (scoreView != null) scoreView.setText(I18n.f("jump_score", s));
            }

            @Override
            public void onGameOver(int s) {
                if (s > best) {
                    best = s;
                    getSharedPreferences("chatapp_jump", Context.MODE_PRIVATE).edit().putInt("best", best).apply();
                    if (bestView != null) bestView.setText(I18n.f("jump_best", best));
                }
                showGameOver(s);
            }

            @Override
            public void onFirstTap() {
                if (hintView != null) hintView.setVisibility(View.GONE);
            }
        });
        root.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 6));
        TextView back = Utils.tv(this, I18n.t("返回", "返回", "Back"), 14, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 14), Utils.dp(this, 7), Utils.dp(this, 14), Utils.dp(this, 7));
        back.setBackground(Utils.bg(this, Utils.CARD, 16));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("jump_title"), 17, Utils.TEXT, Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bestView = Utils.tv(this, I18n.f("jump_best", best), 13, Utils.TEXT_DIM, Gravity.CENTER);
        bestView.setPadding(Utils.dp(this, 10), Utils.dp(this, 7), Utils.dp(this, 10), Utils.dp(this, 7));
        bestView.setBackground(Utils.bg(this, Utils.CARD, 16));
        top.addView(bestView);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        root.addView(top, topLp);

        scoreView = Utils.tv(this, I18n.f("jump_score", 0), 52, Utils.TEXT, Gravity.CENTER);
        scoreView.setTypeface(Typeface.DEFAULT_BOLD);
        scoreView.setShadowLayer(6, 0, 2, 0x66000000);
        FrameLayout.LayoutParams scoreLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        scoreLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        scoreLp.topMargin = Utils.dp(this, 78);
        root.addView(scoreView, scoreLp);

        hintView = Utils.tv(this, I18n.t("jump_howto"), 13, 0xCCFFFFFF, Gravity.CENTER);
        hintView.setPadding(Utils.dp(this, 14), Utils.dp(this, 8), Utils.dp(this, 14), Utils.dp(this, 8));
        hintView.setBackground(Utils.bg(this, 0x55000000, 16));
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        hintLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        hintLp.bottomMargin = Utils.dp(this, 60);
        root.addView(hintView, hintLp);

        overPanel = new FrameLayout(this);
        overPanel.setVisibility(View.GONE);
        overPanel.setBackgroundColor(0x99000000);
        root.addView(overPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        buildOverPanel();
        setContentView(root);
    }
    private void buildOverPanel() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setMinimumWidth(Utils.dp(this, 250));
        card.setBackground(Utils.bg(this, 0xFF232329, 22));
        card.setPadding(Utils.dp(this, 30), Utils.dp(this, 28), Utils.dp(this, 30), Utils.dp(this, 28));
        TextView overTitle = Utils.tv(this, I18n.t("jump_game_over"), 22, Color.WHITE, Gravity.CENTER);
        overTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(overTitle);
        card.addView(Utils.hSpace(this, 14));
        overResult = Utils.tv(this, "", 17, Color.WHITE, Gravity.CENTER);
        overResult.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(overResult);
        card.addView(Utils.hSpace(this, 6));
        overBest = Utils.tv(this, "", 13, 0xFFB8B8C4, Gravity.CENTER);
        card.addView(overBest);
        card.addView(Utils.hSpace(this, 22));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        TextView restart = Utils.tv(this, I18n.t("jump_restart"), 14, Color.WHITE, Gravity.CENTER);
        restart.setPadding(Utils.dp(this, 22), Utils.dp(this, 9), Utils.dp(this, 22), Utils.dp(this, 9));
        restart.setBackground(Utils.bg(this, Utils.ACCENT, 18));
        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartGame();
            }
        });
        row.addView(restart);
        row.addView(Utils.vSpace(this, 12));
        TextView back = Utils.tv(this, I18n.t("返回", "返回", "Back"), 14, Color.WHITE, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 22), Utils.dp(this, 9), Utils.dp(this, 22), Utils.dp(this, 9));
        back.setBackground(Utils.bg(this, 0xFF3A3A44, 18));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        row.addView(back);
        card.addView(row);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        overPanel.addView(card, lp);
    }

    private void showGameOver(int s) {
        overResult.setText(I18n.f("jump_score", s));
        overBest.setText(I18n.f("jump_best", best));
        overPanel.setVisibility(View.VISIBLE);
    }

    private void restartGame() {
        overPanel.setVisibility(View.GONE);
        hintView.setVisibility(View.VISIBLE);
        gameView.restart();
        scoreView.setText(I18n.f("jump_score", 0));
    }
}

class GameView extends View implements Runnable {

    interface Listener {
        void onScore(int s);

        void onGameOver(int s);

        void onFirstTap();
    }

    private static final int SHAPE_TRI = 3;
    private static final int SHAPE_SQUARE = 4;
    private static final int SHAPE_HEX = 6;
    private static final int SHAPE_CYLINDER = 9;
    private static final float CYL_RY = 0.62f;   // 圆柱顶面椭圆的垂直半径比例

    private static final long MAX_CHARGE_MS = 900;
    private static final long JUMP_MS = 320;
    private static final long CAM_MS = 280;
    private static final long FALL_MS = 650;

    private static final int[] PALETTE = {
            0xFFE8A87C, 0xFF85CDCA, 0xFFE27D60, 0xFF41B3A3,
            0xFFC38D9E, 0xFFF2C94C, 0xFF9B5DE5, 0xFF00BBF9
    };

    static class Block {
        final float x;
        final float y;
        final float size;
        final float R;
        final int shape;
        final int color;

        Block(float x, float y, float size, int shape, int color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.shape = shape;
            this.color = color;
            this.R = size * 0.72f;
        }
    }

    private final Listener listener;
    private final Random rnd = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint();
    private final Path path = new Path();

    private final List<Block> blocks = new ArrayList<>();
    private float charX;
    private float charY;
    private float camX;
    private float camY;
    private boolean camAnimating;
    private float camFromX;
    private float camFromY;
    private float camToX;
    private float camToY;
    private long camStart;

    private boolean charging;
    private float power;
    private long chargeStart;

    private boolean jumping;
    private float jDir = 1f;   // 跳跃方向：1 右 / -1 左
    private float jFromX;
    private float jFromY;
    private float jToX;
    private float jToY;
    private float jArc;
    private long jStart;

    private boolean falling;
    private long fallStart;

    private boolean over;
    private int score;
    private boolean firstTap = true;
    private boolean attached;
    private boolean sized;

    private final float baseSize;
    private final float minSize;
    private final float maxSize;
    private final float charW;
    private final float charH;
    private float gapMax = 300f;
    private float maxJump = 340f;
    private float anchorX = 400f;
    private float anchorY = 700f;

    GameView(Context c, Listener l) {
        super(c);
        listener = l;
        baseSize = Utils.dp(c, 52);
        minSize = baseSize * 0.82f;
        maxSize = baseSize * 1.2f;
        charW = baseSize * 0.42f;
        charH = baseSize * 0.6f;
        initGame();
    }

    void restart() {
        initGame();
    }

    private void initGame() {
        blocks.clear();
        score = 0;
        over = false;
        charging = false;
        jumping = false;
        falling = false;
        camAnimating = false;
        power = 0;
        firstTap = true;
        blocks.add(new Block(0f, 0f, randSize(), SHAPE_SQUARE, nextColor()));
        spawnNext();
        charX = 0f;
        charY = 0f;
        if (sized) {
            camX = anchorX - charX;
            camY = anchorY - charY;
        } else {
            camX = anchorX;
            camY = anchorY;
        }
        listener.onScore(0);
        invalidate();
    }

    private void spawnNext() {
        Block cur = blocks.get(blocks.size() - 1);
        Block b = new Block(0f, 0f, randSize(), pickShape(), nextColor());
        float minGap = (cur.R + b.R) * 0.72f + Utils.dp(getContext(), 6);
        float maxGap = Math.max(gapMax, minGap * 1.25f);
        float gap = minGap + rnd.nextFloat() * (maxGap - minGap);
        float dir = rnd.nextBoolean() ? 1f : -1f; // 右上/左上各 50%
        blocks.add(new Block(cur.x + gap * dir, cur.y - gap, b.size, b.shape, b.color));
    }

    private float randSize() {
        return minSize + rnd.nextFloat() * (maxSize - minSize);
    }

    private int pickShape() {
        int r = rnd.nextInt(4);
        if (r == 0) return SHAPE_TRI;
        if (r == 1) return SHAPE_HEX;
        if (r == 2) return SHAPE_CYLINDER;
        return SHAPE_SQUARE;
    }

    private int nextColor() {
        return rnd.nextInt(PALETTE.length);
    }
    @Override
    protected void onSizeChanged(int w, int hh, int oldw, int oldh) {
        super.onSizeChanged(w, hh, oldw, oldh);
        anchorX = w * 0.60f;
        anchorY = hh * 0.74f;
        float margin = Utils.dp(getContext(), 6);
        float room = Math.min(w - anchorX, anchorY) - maxSize * 0.72f - margin;
        if (room > 0) {
            gapMax = room;
            maxJump = gapMax * 1.16f;
        }
        boolean dark = Theme.isDark(getContext());
        int top = dark ? 0xFF191922 : 0xFFD9EAF7;
        int bottom = dark ? 0xFF08080B : 0xFFF6FAFE;
        bgPaint.setShader(new LinearGradient(0, 0, 0, hh, top, bottom, Shader.TileMode.CLAMP));
        if (!sized) {
            sized = true;
            initGame();
        }
    }

    @Override
    public void run() {
        if (!attached) return;
        long now = System.currentTimeMillis();
        update(now);
        invalidate();
        postDelayed(this, 16);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        post(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        removeCallbacks(this);
        super.onDetachedFromWindow();
    }

    private void update(long now) {
        if (over) return;
        if (charging) {
            power = Math.min(1f, (now - chargeStart) / (float) MAX_CHARGE_MS);
        }
        if (falling) {
            charY += 7f;
            if (now - fallStart >= FALL_MS) {
                falling = false;
                over = true;
                listener.onGameOver(score);
            }
            return;
        }
        if (jumping) {
            float t = Math.min(1f, (now - jStart) / (float) JUMP_MS);
            float e = easeOut(t);
            float baseX = jFromX + (jToX - jFromX) * e;
            float baseY = jFromY + (jToY - jFromY) * e;
            float arc = 4f * jArc * t * (1f - t);
            charX = baseX - arc * jDir;
            charY = baseY - arc;
            if (t >= 1f) {
                jumping = false;
                onLand();
            }
        }
        if (camAnimating) {
            float t = Math.min(1f, (now - camStart) / (float) CAM_MS);
            float e = easeInOut(t);
            camX = camFromX + (camToX - camFromX) * e;
            camY = camFromY + (camToY - camFromY) * e;
            if (t >= 1f) camAnimating = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (over) return true;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (jumping || falling) return true;
                if (firstTap) {
                    firstTap = false;
                    listener.onFirstTap();
                }
                charging = true;
                chargeStart = System.currentTimeMillis();
                power = 0;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (charging) {
                    charging = false;
                    doJump();
                }
                return true;
            default:
                return true;
        }
    }

    private void doJump() {
        float d = maxJump * Math.max(0.06f, power);
        float dir = 1f;
        if (blocks.size() > 1) {
            float dx = blocks.get(1).x - charX;
            if (dx != 0f) dir = Math.signum(dx);
        }
        jFromX = charX;
        jFromY = charY;
        jDir = dir;
        jToX = charX + d * dir;
        jToY = charY - d;
        jArc = Utils.dp(getContext(), 55) * power;
        jStart = System.currentTimeMillis();
        jumping = true;
    }

    private void onLand() {
        float lx = jToX;
        float ly = jToY;
        Block cur = blocks.get(0);
        Block next = blocks.get(1);
        if (inside(cur, lx, ly)) {
            charX = lx;
            charY = ly;
            moveCamToChar();
        } else if (inside(next, lx, ly)) {
            score++;
            charX = lx;
            charY = ly;
            blocks.remove(0);
            spawnNext();
            moveCamToChar();
            listener.onScore(score);
        } else {
            falling = true;
            fallStart = System.currentTimeMillis();
        }
    }

    private void moveCamToChar() {
        camFromX = camX;
        camFromY = camY;
        camToX = anchorX - charX;
        camToY = anchorY - charY;
        camAnimating = true;
        camStart = System.currentTimeMillis();
    }

    private boolean inside(Block b, float px, float py) {
        float dx = px - b.x;
        float dy = py - b.y;
        float R = b.R + charW * 0.35f;
        switch (b.shape) {
            case SHAPE_CYLINDER: {
                float rx = b.R;
                float ry = b.R * CYL_RY;
                return dx * dx / (rx * rx) + dy * dy / (ry * ry) <= 1f;
            }
            case SHAPE_TRI:
                return pointInTri(dx, dy, 0f, -R, 0.866f * R, 0.5f * R, -0.866f * R, 0.5f * R);
            case SHAPE_HEX:
                return pointInPoly(dx, dy, hexVerts(R));
            default:
                return Math.abs(dx) + Math.abs(dy) <= R;
        }
    }

    private static boolean pointInTri(float px, float py, float ax, float ay, float bx, float by, float cx, float cy) {
        float d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        float d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        float d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0;
        boolean pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    private static boolean pointInPoly(float px, float py, float[] v) {
        boolean inside = false;
        int n = v.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = v[i * 2];
            float yi = v[i * 2 + 1];
            float xj = v[j * 2];
            float yj = v[j * 2 + 1];
            if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static float[] hexVerts(float R) {
        float[] v = new float[12];
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(-90 + i * 60);
            v[i * 2] = (float) (R * Math.cos(a));
            v[i * 2 + 1] = (float) (R * Math.sin(a));
        }
        return v;
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u;
    }

    private static float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
    }
    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        for (Block b : blocks) drawBlock(c, b);
        drawChar(c);
        if (charging && !jumping) drawPower(c);
    }

    private void drawBlock(Canvas c, Block b) {
        float x = b.x + camX;
        float y = b.y + camY;
        float R = b.R;
        float h = b.size * 0.30f;
        int top = PALETTE[b.color];
        if (b.shape == SHAPE_CYLINDER) {
            drawCylinder(c, x, y, R, h, top);
            return;
        }
        int n = b.shape;
        float[] v = topVerts(n, R);
        int first = b.shape == SHAPE_TRI ? 0 : 1;
        int last = b.shape == SHAPE_TRI ? n : n - 1;
        for (int i = first; i < last; i++) {
            float x1 = v[i * 2];
            float y1 = v[i * 2 + 1];
            float x2 = v[((i + 1) % n) * 2];
            float y2 = v[((i + 1) % n) * 2 + 1];
            path.reset();
            path.moveTo(x + x1, y + y1);
            path.lineTo(x + x2, y + y2);
            path.lineTo(x + x2, y + y2 + h);
            path.lineTo(x + x1, y + y1 + h);
            path.close();
            paint.setColor(shade(top, 0.56f + 0.08f * (i % 2)));
            c.drawPath(path, paint);
        }
        path.reset();
        path.moveTo(x + v[0], y + v[1]);
        for (int i = 1; i < n; i++) path.lineTo(x + v[i * 2], y + v[i * 2 + 1]);
        path.close();
        paint.setColor(top);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Utils.dp(getContext(), 2.5f));
        paint.setColor(shade(top, 0.72f));
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCylinder(Canvas c, float x, float y, float R, float h, int top) {
        float ry = R * CYL_RY;
        // 侧面：左竖线 + 底部椭圆弧 + 右竖线 + 顶部椭圆弧
        path.reset();
        path.moveTo(x - R, y);
        path.lineTo(x - R, y + h);
        path.arcTo(new RectF(x - R, y + h - ry, x + R, y + h + ry), 180f, -180f);
        path.lineTo(x + R, y);
        path.arcTo(new RectF(x - R, y - ry, x + R, y + ry), 0f, 180f);
        path.close();
        paint.setColor(shade(top, 0.66f));
        c.drawPath(path, paint);
        paint.setColor(top);
        c.drawOval(new RectF(x - R, y - ry, x + R, y + ry), paint);
    }
    private static float[] topVerts(int n, float R) {
        if (n == SHAPE_TRI) {
            return new float[]{0f, -R, 0.866f * R, 0.5f * R, -0.866f * R, 0.5f * R};
        }
        if (n == SHAPE_HEX) {
            return hexVerts(R);
        }
        return new float[]{0f, -R, R, 0f, 0f, R, -R, 0f};
    }

    private void drawChar(Canvas c) {
        float sx = charX + camX;
        float sy = charY + camY;
        float w = charW;
        float hh = charH;
        c.save();
        if (charging) {
            float k = power;
            w = charW * (1f + 0.22f * k);
            hh = charH * (1f - 0.28f * k);
        } else if (jumping || falling) {
            float t = 1f;
            if (jumping) t = Math.min(1f, (System.currentTimeMillis() - jStart) / (float) JUMP_MS);
            c.rotate(55f * t * jDir, sx, sy);
        }
        shadowPaint.setColor(0x33000000);
        c.drawOval(new RectF(sx - w * 0.66f, sy - 5, sx + w * 0.66f, sy + 3), shadowPaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF2F4F8);
        RectF body = new RectF(sx - w / 2f, sy - hh, sx + w / 2f, sy);
        c.drawRoundRect(body, w / 2f, w / 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(0xFF3A3F4A);
        c.drawRoundRect(body, w / 2f, w / 2f, paint);
        paint.setStyle(Paint.Style.FILL);
        float ey = sy - hh * 0.55f;
        paint.setColor(0xFF2A2E36);
        c.drawCircle(sx - w * 0.20f, ey, w * 0.075f, paint);
        c.drawCircle(sx + w * 0.20f, ey, w * 0.075f, paint);
        paint.setColor(0x88FFFFFF);
        c.drawOval(new RectF(sx - w * 0.30f, sy - hh * 0.88f, sx + w * 0.30f, sy - hh * 0.64f), paint);
        c.restore();
    }

    private void drawPower(Canvas c) {
        float w = 140f;
        float hh = 8f;
        float x0 = getWidth() / 2f - w / 2f;
        float y0 = getHeight() - Utils.dp(getContext(), 64);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33000000);
        c.drawRoundRect(new RectF(x0, y0, x0 + w, y0 + hh), hh, hh, paint);
        paint.setColor(Theme.isDark(getContext()) ? 0xFF37D367 : 0xFF1A73E8);
        c.drawRoundRect(new RectF(x0 + 1, y0 + 1, x0 + 1 + (w - 2) * power, y0 + hh - 1), hh, hh, paint);
    }

    private static int shade(int color, float f) {
        int a = (color >> 24) & 0xff;
        int rr = (int) (((color >> 16) & 0xff) * f);
        int gg = (int) (((color >> 8) & 0xff) * f);
        int bb = (int) ((color & 0xff) * f);
        return Color.argb(a, rr, gg, bb);
    }
}
