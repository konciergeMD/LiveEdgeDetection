package com.adityaarora.liveedgedetection.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import com.adityaarora.liveedgedetection.R
import com.adityaarora.liveedgedetection.activity.ScanActivity
import kotlinx.android.synthetic.main.fragment_rotate.*

/**
 * Fragment that handles rotating an image and then saving it
 *
 * Use the [RotateFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RotateFragment : Fragment() {
    private lateinit var bitmap: Bitmap
    private var currentRotation: Float = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rotate, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rotate_image_view.setImageBitmap(bitmap)

        rotate_rotate_btn.setOnClickListener {v ->
            onRotateButtonClick(v)
        }

        rotate_save_btn.setOnClickListener { v ->
            onSaveButtonClick(v)
        }

        rotate_reject_btn.setOnClickListener { v ->
            fragmentManager.popBackStack()
        }
    }

    /**
     * Rotates the cropped image view, but not the bitmap itself
     *
     * @param v view from button's onClick
     */
    private fun onRotateButtonClick(v: View) {
        // Update the rotation using a cool rotation animation
        val rotateAnimation = RotateAnimation(currentRotation, currentRotation - 90f,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f)

        // Non-null assertion here should be okay because we JUST updated it
        rotateAnimation.interpolator = AccelerateDecelerateInterpolator() // Nice and smooth
        rotateAnimation.duration = 250 // 250 milliseconds
        rotateAnimation.fillAfter = true
        rotateAnimation.isFillEnabled = true

        // Listener for enabling and disabling the rotation button
        rotateAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                rotate_rotate_btn.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animation) {
                rotate_rotate_btn.isEnabled = true
                currentRotation -= 90f
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        rotate_image_view!!.animation = rotateAnimation
        rotate_image_view.startAnimation(rotateAnimation)
    }

    /**
     * Save the image
     *
     * @param v view from button's onCLick
     */
    private fun onSaveButtonClick(v: View) {
        if (activity is ScanActivity) {
            this.bitmap = (activity as ScanActivity).rotateBitmap(bitmap, currentRotation.toInt())
            (activity as ScanActivity).saveImage(this.bitmap)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param bitmap cropped bitmap
         * @return A new instance of fragment RotateFragment.
         */
        @JvmStatic
        fun newInstance(bitmap: Bitmap) =
                RotateFragment().apply {
                    this.bitmap = bitmap
                }
    }
}
