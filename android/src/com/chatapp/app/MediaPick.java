package com.chatapp.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;

/** 拍照/相册选择 + 拍照后编辑(旋转/裁剪/重拍) 统一组件 */
public class MediaPick {

    public interface Callback {
        void onBitmap(Bitmap bmp);
    }

    private static Callback pending;
    private static int lastRequest = -1;
    private static int pendingCameraRequest = -1;
    private static Uri cameraUri;
    private static File cameraFile;

    /** 弹窗选择: 拍照 / 从相册选择 */
    public static void choose(final Activity a, final int requestCode, final Callback cb) {
        pending = cb;
        lastRequest = requestCode;
        Ui.list(a, I18n.t("choose_source"), new String[]{I18n.t("take_photo"), I18n.t("choose_image")},
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                takePhoto(a, requestCode);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                pickGallery(a, requestCode);
                            }
                        }
                });
    }

    /** 直接唤起相机(扫码等场景) */
    public static void takePhoto(Activity a, int requestCode) {
        lastRequest = requestCode;
        if (Build.VERSION.SDK_INT >= 23
                && a.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingCameraRequest = requestCode;
            a.requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 9001);
            return;
        }
        launchCamera(a, requestCode);
    }

    private static void launchCamera(Activity a, int requestCode) {
        try {
            File dir = a.getCacheDir();
            cameraFile = new File(dir, "capture_" + System.currentTimeMillis() + ".jpg");
            cameraUri = Uri.parse("content://com.chatapp.app.media/" + Uri.encode(cameraFile.getName()));
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivityForResult(i, requestCode);
        } catch (Exception e) {
            Ui.popup(a, I18n.t("cannot_open_camera"));
        }
    }

    public static void pickGallery(Activity a, int requestCode) {
        lastRequest = requestCode;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        try {
            a.startActivityForResult(Intent.createChooser(i, I18n.t("choose_image")), requestCode);
        } catch (Exception e) {
            Ui.popup(a, I18n.t("cannot_open_picker"));
        }
    }

    /** 相机权限结果 */
    public static void onPermissionResult(Activity a, int req, int[] results) {
        if (req == 9001) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCameraRequest >= 0) {
                    int r = pendingCameraRequest;
                    pendingCameraRequest = -1;
                    launchCamera(a, r);
                }
            } else {
                pendingCameraRequest = -1;
                pending = null;
                lastRequest = -1;
            }
        }
    }

    /** 在 Activity.onActivityResult 中调用, 处理拍照/相册结果; 返回 true 表示已消费 */
    public static boolean handleResult(Activity a, int requestCode, int resultCode, Intent data) {
        if (requestCode != lastRequest) return false;
        lastRequest = -1;
        final Callback cb = pending;
        pending = null;
        scanMode = false;
        if (cb == null) return true;
        if (resultCode != Activity.RESULT_OK) return true;
        Bitmap bmp = null;
        if (data != null && data.getData() != null) {
            try {
                InputStream in = a.getContentResolver().openInputStream(data.getData());
                bmp = BitmapFactory.decodeStream(in);
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
        } else if (cameraFile != null && cameraFile.exists()) {
            bmp = BitmapFactory.decodeFile(cameraFile.getAbsolutePath());
        }
        if (bmp == null) {
            Ui.popup(a, I18n.t("cannot_read_image"));
            return true;
        }
        boolean fromCamera = (data == null || data.getData() == null);
        if (fromCamera && !scanMode) {
            showEditor(a, bmp, cb);
        } else {
            cb.onBitmap(bmp);
        }
        return true;
    }

    /** 扫码模式: 拍照后直接回调原图, 不进编辑器 */
    public static void scan(Activity a, int requestCode, Callback cb) {
        scanMode = true;
        pending = cb;
        lastRequest = requestCode;
        takePhoto(a, requestCode);
    }

    private static boolean scanMode = false;

    // ---------- 拍照后编辑: 旋转 / 裁剪 / 重拍 / 取消 ----------

    private static void showEditor(final Activity a, final Bitmap src, final Callback cb) {
        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(Utils.BG);

        final ImageView iv = new ImageView(a);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(iv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final CropOverlay overlay = new CropOverlay(a);
        root.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final Bitmap[] cur = {src};
        iv.setImageBitmap(cur[0]);

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(Utils.dp(a, 8), Utils.dp(a, 10), Utils.dp(a, 8), Utils.dp(a, 10));
        bar.addView(barBtn(a, I18n.t("rotate"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matrix m = new Matrix();
                m.postRotate(90);
                Bitmap r = Bitmap.createBitmap(cur[0], 0, 0, cur[0].getWidth(), cur[0].getHeight(), m, true);
                if (r != cur[0] && !cur[0].isRecycled()) cur[0].recycle();
                cur[0] = r;
                iv.setImageBitmap(cur[0]);
            }
        }));
        bar.addView(Utils.vSpace(a, 10));
        bar.addView(barBtn(a, I18n.t("retake"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                scanMode = false;
                pending = cb;
                lastRequest = 9002;
                launchCamera(a, 9002);
            }
        }));
        bar.addView(Utils.vSpace(a, 10));
        bar.addView(barBtn(a, I18n.t("cancel"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        }));
        bar.addView(Utils.vSpace(a, 10));
        bar.addView(barBtn(a, I18n.t("done"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                Bitmap b = cur[0];
                if (b == null || b.isRecycled()) return;
                int side = Math.min(b.getWidth(), b.getHeight());
                int sx = (b.getWidth() - side) / 2;
                int sy = (b.getHeight() - side) / 2;
                Bitmap cropped = Bitmap.createBitmap(b, sx, sy, side, side);
                int target = 1024;
                if (side > target) {
                    Bitmap scaled = Bitmap.createScaledBitmap(cropped, target, target, true);
                    if (scaled != cropped) cropped.recycle();
                    cropped = scaled;
                }
                cb.onBitmap(cropped);
            }
        }));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        blp.bottomMargin = Utils.dp(a, 24);
        root.addView(bar, blp);

        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Utils.BG));
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        d.show();
    }

    private static TextView barBtn(Activity a, String text, View.OnClickListener onClick) {
        TextView t = Utils.tv(a, text, 14, Utils.TEXT, Gravity.CENTER);
        t.setBackground(Utils.bg(a, Utils.CARD2, 16));
        t.setPadding(Utils.dp(a, 18), Utils.dp(a, 8), Utils.dp(a, 18), Utils.dp(a, 8));
        t.setOnClickListener(onClick);
        return t;
    }

    /** 方形裁剪引导: 中心方形亮框 + 四周变暗 */
    public static class CropOverlay extends View {
        private final Paint mask = new Paint();
        private final Paint frame = new Paint();

        public CropOverlay(android.content.Context c) {
            super(c);
            mask.setColor(0x99000000);
            frame.setStyle(Paint.Style.STROKE);
            frame.setStrokeWidth(3);
            frame.setColor(Utils.ACCENT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            int side = (int) (Math.min(w, h) * 0.9f);
            int left = (w - side) / 2, top = (h - side) / 2;
            canvas.drawRect(0, 0, w, top, mask);
            canvas.drawRect(0, top + side, w, h, mask);
            canvas.drawRect(0, top, left, top + side, mask);
            canvas.drawRect(left + side, top, w, top + side, mask);
            canvas.drawRect(left, top, left + side, top + side, frame);
        }
    }
}
