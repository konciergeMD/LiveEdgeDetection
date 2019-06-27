package com.adityaarora.liveedgedetection.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adityaarora.liveedgedetection.R;
import com.adityaarora.liveedgedetection.constants.ScanConstants;
import com.adityaarora.liveedgedetection.enums.ScanHint;
import com.adityaarora.liveedgedetection.interfaces.IScanner;
import com.adityaarora.liveedgedetection.util.ScanUtils;
import com.adityaarora.liveedgedetection.view.PolygonPoints;
import com.adityaarora.liveedgedetection.view.PolygonView;
import com.adityaarora.liveedgedetection.view.Quadrilateral;
import com.adityaarora.liveedgedetection.view.ScanSurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static android.view.View.GONE;

/**
 * This class initiates camera and detects edges on live view
 */
public class ScanActivity extends AppCompatActivity implements IScanner, View.OnClickListener {
    public final static Stack<PolygonPoints> allDraggedPointsStack = new Stack<>();
    private static final String TAG = ScanActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 101;
    private static final String mOpenCvLibrary = "opencv_java3";

    // Load OpenCV -- DO NOT REMOVE THIS OR ELSE YOU WILL HAVE A BAD TIME
    static {
        System.loadLibrary(mOpenCvLibrary);
    }

    private ViewGroup containerScan;
    private FrameLayout cameraPreviewLayout;
    private FrameLayout cropLayout;
    private ScanSurfaceView mImageSurfaceView;
    private boolean isPermissionNotGranted;
    private TextView captureHintText;
    private LinearLayout captureHintLayout;
    private PolygonView polygonView;
    private ImageView cropImageView;
    // Buttons switch up being active
    private View cropAcceptBtn;
    private View cropRejectBtn;
    private View cropSaveBtn;
    private View cropRotateBtn;
    // Bitmap that has been registered as a card/document
    private Bitmap copyBitmap;
    // Bitmap that has been cropped and enhanced
    private Bitmap croppedBitmap;
    // Maintains current rotation (used for rotating the exported bitmap)
    private float currentRotation;

    private Animation rotateAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        init();
    }

    /**
     * WHOA THERE! This is probably the worst possible solution to up navigation!
     * <p>
     * In the future, this should be switching between activities (ScanActivity, CropActivity, RotateActivity) or Fragments (?)
     * Right now, it just goes through some functions that hide/show views in order to match what should be shown
     */
    @Override
    public void onBackPressed() {
        Log.i(TAG, "Back has been pressed");

        // We're in the rotate view, so go back to the crop view
        if (cropSaveBtn.getVisibility() == View.VISIBLE) {
            // Go back to the crop view
            Log.i(TAG, "Goind back to crop view");
            activateCropView();
//            onPictureClicked(copyBitmap);
            // We're in crop view, so go back to the scan view
        } else if (cropAcceptBtn.getVisibility() == View.VISIBLE) {
            // Go back to the scan view
            Log.i(TAG, "Goind back to scan view");
            activateScanView();
        } else {
            // Exit, probably
            super.onBackPressed();
        }
    }

    private void init() {
        containerScan = findViewById(R.id.container_scan);
        cameraPreviewLayout = findViewById(R.id.camera_preview);
        captureHintLayout = findViewById(R.id.capture_hint_layout);
        captureHintText = findViewById(R.id.capture_hint_text);
        polygonView = findViewById(R.id.polygon_view);
        cropImageView = findViewById(R.id.crop_image_view);
        cropAcceptBtn = findViewById(R.id.crop_accept_btn);
        cropRejectBtn = findViewById(R.id.crop_reject_btn);
        cropRotateBtn = findViewById(R.id.crop_rotate_btn);
        cropSaveBtn = findViewById(R.id.crop_save_btn);
        cropLayout = findViewById(R.id.crop_layout);

        currentRotation = 0f;

        // The accept button uses this class' onClick method for some reason TODO: this should be fixed
        cropAcceptBtn.setOnClickListener(this);

        // Reject button click listener
        cropRejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateScanView();
            }
        });

        checkCameraPermissions();
    }

    private void checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            isPermissionNotGranted = true;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Enable camera permission from settings", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            if (!isPermissionNotGranted) {
                mImageSurfaceView = new ScanSurfaceView(ScanActivity.this, this);
                cameraPreviewLayout.addView(mImageSurfaceView);
            } else {
                isPermissionNotGranted = false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                onRequestCamera(grantResults);
                break;
            default:
                break;
        }
    }

    private void onRequestCamera(int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageSurfaceView = new ScanSurfaceView(ScanActivity.this, ScanActivity.this);
                            cameraPreviewLayout.addView(mImageSurfaceView);
                        }
                    });
                }
            }, 500);

        } else {
            Toast.makeText(this, getString(R.string.camera_activity_permission_denied_toast), Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    @Override
    public void displayHint(ScanHint scanHint) {
        captureHintLayout.setVisibility(View.VISIBLE);
        switch (scanHint) {
            case MOVE_CLOSER:
                captureHintText.setText(getResources().getString(R.string.move_closer));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_red));
                break;
            case MOVE_AWAY:
                captureHintText.setText(getResources().getString(R.string.move_away));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_red));
                break;
            case ADJUST_ANGLE:
                captureHintText.setText(getResources().getString(R.string.adjust_angle));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_red));
                break;
            case FIND_RECT:
                captureHintText.setText(getResources().getString(R.string.finding_rect));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_blue));
                break;
            case CAPTURING_IMAGE:
                captureHintText.setText(getResources().getString(R.string.hold_still));
                captureHintLayout.setBackground(getResources().getDrawable(R.drawable.hint_green));
                break;
            case NO_MESSAGE:
                captureHintLayout.setVisibility(GONE);
                break;
            default:
                break;
        }
    }

    /**
     * Shows the cropping activity
     *
     * @param bitmap
     */
    @Override
    public void onPictureClicked(final Bitmap bitmap) {
        try {
            activateCropView();

            copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            int height = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();
            int width = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWidth();

            copyBitmap = ScanUtils.resizeToScreenContentSize(copyBitmap, width, height);

            Mat originalMat = new Mat(copyBitmap.getHeight(), copyBitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(copyBitmap, originalMat);
            ArrayList<PointF> points;
            Map<Integer, PointF> pointFs = new HashMap<>();
            try {
                Quadrilateral quad = ScanUtils.detectLargestQuadrilateral(originalMat);
                if (null != quad) {
                    double resultArea = Math.abs(Imgproc.contourArea(quad.contour));
                    double previewArea = originalMat.rows() * originalMat.cols();
                    if (resultArea > previewArea * 0.08) {
                        points = new ArrayList<>();
                        points.add(new PointF((float) quad.points[0].x, (float) quad.points[0].y));
                        points.add(new PointF((float) quad.points[1].x, (float) quad.points[1].y));
                        points.add(new PointF((float) quad.points[3].x, (float) quad.points[3].y));
                        points.add(new PointF((float) quad.points[2].x, (float) quad.points[2].y));
                    } else {
                        points = ScanUtils.getPolygonDefaultPoints(copyBitmap);
                    }

                } else {
                    points = ScanUtils.getPolygonDefaultPoints(copyBitmap);
                }

                int index = -1;
                for (PointF pointF : points) {
                    pointFs.put(++index, pointF);
                }

                polygonView.setPoints(pointFs);
                int padding = (int) getResources().getDimension(R.dimen.scan_padding);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(copyBitmap.getWidth() + 2 * padding, copyBitmap.getHeight() + 2 * padding);
                layoutParams.gravity = Gravity.CENTER;
                polygonView.setLayoutParams(layoutParams);

                // Update the crop image view size
                cropImageView.setLayoutParams(new FrameLayout.LayoutParams(copyBitmap.getWidth(), copyBitmap.getHeight()));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    TransitionManager.beginDelayedTransition(containerScan);
                cropLayout.setVisibility(View.VISIBLE);

                cropImageView.setImageBitmap(copyBitmap);
                cropImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Accept the crop, start the rotation activity
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        activateRotateView();
    }

    /**
     * Rotates the cropped image view, but not the bitmap itself
     *
     * @param v view from button's onClick
     */
    public void onRotateButtonClick(View v) {
        // Update the rotation using a cool rotation animation
        rotateAnimation = new RotateAnimation(currentRotation, currentRotation - 90f,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        rotateAnimation.setInterpolator(new AccelerateDecelerateInterpolator()); // Nice and smooth
        rotateAnimation.setDuration(250); // 250 milliseconds
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setFillEnabled(true);

        // Listener for enabling and disabling the rotation button
        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                cropRotateBtn.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cropRotateBtn.setEnabled(true);
                currentRotation -= 90;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        cropImageView.setAnimation(rotateAnimation);
        cropImageView.startAnimation(rotateAnimation);
    }

    /**
     * Saves the image and finishes the activity
     *
     * @param v
     */
    public void saveImage(View v) {
        // Since there is no way back out of saving the image, we can just rotate the cropped bitmap
        croppedBitmap = rotateBitmap(croppedBitmap, (int) currentRotation);
        String path = ScanUtils.saveToInternalMemory(croppedBitmap, ScanConstants.IMAGE_DIR,
                ScanConstants.IMAGE_NAME, ScanActivity.this, 90)[0];
        setResult(Activity.RESULT_OK, new Intent().putExtra(ScanConstants.SCANNED_RESULT, path));
        System.gc();
        finish();
    }

    /**
     * Rotates bitmap 90 degrees
     *
     * @param bitmap
     * @return rotated bitmap
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
    }

    // ====== Helper methods for the horrible back navigation =======

    private void activateCropView() {
        // Show correct information for crop activity
        cropImageView.setVisibility(View.VISIBLE);
        polygonView.setVisibility(View.VISIBLE);
        cameraPreviewLayout.setVisibility(GONE);
        cropRejectBtn.setVisibility(View.VISIBLE);
        cropRotateBtn.setVisibility(GONE);
        cropAcceptBtn.setVisibility(View.VISIBLE);
        cropSaveBtn.setVisibility(GONE);
        cropImageView.clearAnimation();
    }

    private void activateRotateView() {
        Map<Integer, PointF> points = polygonView.getPoints();
        if (ScanUtils.isScanPointsValid(points)) {
            Point point1 = new Point(points.get(0).x, points.get(0).y);
            Point point2 = new Point(points.get(1).x, points.get(1).y);
            Point point3 = new Point(points.get(2).x, points.get(2).y);
            Point point4 = new Point(points.get(3).x, points.get(3).y);
            croppedBitmap = ScanUtils.enhanceReceipt(copyBitmap, point1, point2, point3, point4);
        }
        cropImageView.setAnimation(rotateAnimation);
        mImageSurfaceView.setVisibility(View.INVISIBLE);
        cropImageView.setImageBitmap(croppedBitmap);
        cropRotateBtn.setVisibility(View.VISIBLE);
        cropSaveBtn.setVisibility(View.VISIBLE);
        cropRejectBtn.setVisibility(View.VISIBLE);
        cropAcceptBtn.setVisibility(GONE);
        polygonView.setVisibility(GONE);
        cropImageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private void activateScanView() {
        currentRotation = 0f;
        polygonView.setVisibility(GONE);
        cropImageView.setVisibility(View.GONE);
        cameraPreviewLayout.setVisibility(View.VISIBLE);
        mImageSurfaceView.setVisibility(View.VISIBLE);
        cropAcceptBtn.setVisibility(GONE);
        cropSaveBtn.setVisibility(GONE);
        cropLayout.setVisibility(GONE);
        cropRotateBtn.setVisibility(GONE);
        cropRejectBtn.setVisibility(GONE);
        mImageSurfaceView.setPreviewCallback();
        copyBitmap.recycle();
    }
}
