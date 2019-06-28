package com.adityaarora.liveedgedetection.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.adityaarora.liveedgedetection.R
import com.adityaarora.liveedgedetection.constants.ScanConstants
import com.adityaarora.liveedgedetection.fragments.ScanFragment
import com.adityaarora.liveedgedetection.util.ScanUtils
import com.adityaarora.liveedgedetection.view.PolygonPoints
import com.adityaarora.liveedgedetection.view.PolygonView
import com.adityaarora.liveedgedetection.view.ScanSurfaceView

import java.util.Stack

/**
 * This class initiates camera and detects edges on live view
 */
class ScanActivity : FragmentActivity() {
    private val cropImageView: ImageView? = null
    // Buttons switch up being active
    private val cropRotateBtn: View? = null
    // Bitmap that has been cropped and enhanced
    private var croppedBitmap: Bitmap? = null
    // Maintains current rotation (used for rotating the exported bitmap)
    private var currentRotation: Float = 0.toFloat()

    private var rotateAnimation: Animation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
    }

    override fun onStart() {
        super.onStart()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val newFragment = ScanFragment()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.base_layout, newFragment)
        ft.commit()
    }

    /**
     * Rotates the cropped image view, but not the bitmap itself
     *
     * @param v view from button's onClick
     */
    fun onRotateButtonClick(v: View) {
        // Update the rotation using a cool rotation animation
        rotateAnimation = RotateAnimation(currentRotation, currentRotation - 90f,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f)
        rotateAnimation!!.interpolator = AccelerateDecelerateInterpolator() // Nice and smooth
        rotateAnimation!!.duration = 250 // 250 milliseconds
        rotateAnimation!!.fillAfter = true
        rotateAnimation!!.isFillEnabled = true

        // Listener for enabling and disabling the rotation button
        rotateAnimation!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                cropRotateBtn!!.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animation) {
                cropRotateBtn!!.isEnabled = true
                currentRotation -= 90f
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        cropImageView!!.animation = rotateAnimation
        cropImageView.startAnimation(rotateAnimation)
    }

    /**
     * Saves the image and finishes the activity
     *
     * @param v
     */
    fun saveImage(v: View) {
        // Since there is no way back out of saving the image, we can just rotate the cropped bitmap
        croppedBitmap = rotateBitmap(croppedBitmap, currentRotation.toInt())
        val path = ScanUtils.saveToInternalMemory(croppedBitmap, ScanConstants.IMAGE_DIR,
                ScanConstants.IMAGE_NAME, this, 90)[0]
        setResult(Activity.RESULT_OK, Intent().putExtra(ScanConstants.SCANNED_RESULT, path))
        System.gc()
        finish()
    }

    /**
     * Rotates bitmap 90 degrees
     *
     * @param bitmap
     * @return rotated bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap?, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap!!, bitmap.width, bitmap.height, true)
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
    }

    companion object {
        val allDraggedPointsStack = Stack<PolygonPoints>()
        private val TAG = ScanActivity::class.java.simpleName
        private val mOpenCvLibrary = "opencv_java3"

        // Load OpenCV -- DO NOT REMOVE THIS OR ELSE YOU WILL HAVE A BAD TIME
        init {
            System.loadLibrary(mOpenCvLibrary)
        }
    }
}
