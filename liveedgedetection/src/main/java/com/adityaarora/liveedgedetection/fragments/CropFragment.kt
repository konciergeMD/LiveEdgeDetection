package com.adityaarora.liveedgedetection.fragments

import android.support.v4.app.Fragment
import android.graphics.Bitmap
import org.opencv.core.Point
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import com.adityaarora.liveedgedetection.R

import com.adityaarora.liveedgedetection.util.ScanUtils
import kotlinx.android.synthetic.main.fragment_crop.*
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.ArrayList
import java.util.HashMap

/**
 * Fragment for cropping the image
 */
class CropFragment : Fragment() {
    private lateinit var bitmap: Bitmap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_crop, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        crop_layout.visibility = View.VISIBLE

        crop_reject_btn.setOnClickListener {
            activity.fragmentManager.popBackStack()
        }

        crop_accept_btn.setOnClickListener {
            val points = polygon_view.points

            // We need to keep this.bitmap intact in case the user goes back
            var copyBitmap: Bitmap = this.bitmap

            // Crop and flatten it to look pretty
            if (ScanUtils.isScanPointsValid(points)) {
                val point1 = Point(points[0]?.x!!.toDouble(), points[0]?.y!!.toDouble())
                val point2 = Point(points[1]?.x!!.toDouble(), points[1]?.y!!.toDouble())
                val point3 = Point(points[2]?.x!!.toDouble(), points[2]?.y!!.toDouble())
                val point4 = Point(points[3]?.x!!.toDouble(), points[3]?.y!!.toDouble())
                copyBitmap = ScanUtils.enhanceReceipt(bitmap, point1, point2, point3, point4)
            }

            val rotateFragment = RotateFragment.newInstance(copyBitmap)
            val fm = activity.supportFragmentManager
            val transaction = fm.beginTransaction()
            transaction.replace(R.id.base_layout, rotateFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        crop_reject_btn.setOnClickListener {
            fragmentManager.popBackStack()
        }

        cropAndEnhanceImage(bitmap)
    }

    /**
     * Shows the cropping activity
     *
     * @param bitmap
     */
    private fun cropAndEnhanceImage(bitmap: Bitmap) {
        try {
            var copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            val height = activity.window.findViewById<View>(Window.ID_ANDROID_CONTENT).height
            val width = activity.window.findViewById<View>(Window.ID_ANDROID_CONTENT).width

            copyBitmap = ScanUtils.resizeToScreenContentSize(copyBitmap, width, height)

            val originalMat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC1)
            Utils.bitmapToMat(copyBitmap, originalMat)
            val points: ArrayList<PointF>
            val pointFs = HashMap<Int, PointF>()
            try {
                val quad = ScanUtils.detectLargestQuadrilateral(originalMat)
                if (null != quad) {
                    val resultArea = Math.abs(Imgproc.contourArea(quad.contour))
                    val previewArea = (originalMat.rows() * originalMat.cols()).toDouble()
                    if (resultArea > previewArea * 0.08) {
                        points = ArrayList()
                        points.add(PointF(quad.points[0].x.toFloat(), quad.points[0].y.toFloat()))
                        points.add(PointF(quad.points[1].x.toFloat(), quad.points[1].y.toFloat()))
                        points.add(PointF(quad.points[3].x.toFloat(), quad.points[3].y.toFloat()))
                        points.add(PointF(quad.points[2].x.toFloat(), quad.points[2].y.toFloat()))
                    } else {
                        points = ScanUtils.getPolygonDefaultPoints(copyBitmap)
                    }

                } else {
                    points = ScanUtils.getPolygonDefaultPoints(copyBitmap)
                }

                var index = -1
                for (pointF in points) {
                    pointFs[++index] = pointF
                }
                polygon_view.points = pointFs
                val padding = resources.getDimension(R.dimen.scan_padding).toInt()
                val layoutParams = FrameLayout.LayoutParams(copyBitmap.width + 2 * padding, copyBitmap.height + 2 * padding)
                layoutParams.gravity = Gravity.CENTER
                polygon_view.setLayoutParams(layoutParams)

                // Update the crop image view size
                crop_image_view.layoutParams = FrameLayout.LayoutParams(copyBitmap.width, copyBitmap.height)

                crop_layout.visibility = View.VISIBLE

                crop_image_view.setImageBitmap(copyBitmap)

                // Overwrite the old bitmap with the new cool one
                this.bitmap = copyBitmap

                crop_image_view.scaleType = ImageView.ScaleType.FIT_XY
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    companion object {
        private val TAG = CropFragment::class.java.simpleName

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param bitmap UNCROPPED bitmap
         * @return A new instance of fragment CropFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(bitmap: Bitmap) =
                CropFragment().apply {
                    this.bitmap = bitmap
                }
    }
}
