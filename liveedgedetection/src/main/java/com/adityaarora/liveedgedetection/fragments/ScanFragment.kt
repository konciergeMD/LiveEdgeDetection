package com.adityaarora.liveedgedetection.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.adityaarora.liveedgedetection.R
import com.adityaarora.liveedgedetection.activity.ScanActivity
import com.adityaarora.liveedgedetection.enums.ScanHint
import com.adityaarora.liveedgedetection.interfaces.IScanner
import com.adityaarora.liveedgedetection.view.ScanSurfaceView
import kotlinx.android.synthetic.main.fragment_scan.*

private const val MY_PERMISSIONS_REQUEST_CAMERA = 101

/**
 * A simple [Fragment] subclass.
 * Use the [ScanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScanFragment : Fragment(), IScanner {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var isPermissionNotGranted: Boolean = false
    private var mImageSurfaceView: ScanSurfaceView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCameraPermissions()
    }

    private fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            isPermissionNotGranted = true
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.CAMERA)) {
                Toast.makeText(activity, "Enable camera permission from settings", Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.CAMERA),
                        MY_PERMISSIONS_REQUEST_CAMERA)
            }
        } else {
            if (!isPermissionNotGranted) {
                mImageSurfaceView = ScanSurfaceView(context, this)
                camera_preview.addView(mImageSurfaceView)
            } else {
                isPermissionNotGranted = false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> onRequestCamera(grantResults)
            else -> {
            }
        }
    }

    private fun onRequestCamera(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Handler().postDelayed({
                mImageSurfaceView = ScanSurfaceView(activity, this)
                camera_preview.addView(mImageSurfaceView)
            }, 500)

        } else {
            Toast.makeText(activity, getString(R.string.camera_activity_permission_denied_toast), Toast.LENGTH_SHORT).show()
            activity.finish()
        }
    }

    /**
     * Updates the hint so users know how to best scan
     * @param scanHint
     */
    override fun displayHint(scanHint: ScanHint) {
        capture_hint_layout.setVisibility(View.VISIBLE)
        when (scanHint) {
            ScanHint.MOVE_CLOSER -> {
                capture_hint_text.text = resources.getString(R.string.move_closer)
                capture_hint_layout.background = resources.getDrawable(R.drawable.hint_red)
            }
            ScanHint.MOVE_AWAY -> {
                capture_hint_text.text = resources.getString(R.string.move_away)
                capture_hint_layout.background = resources.getDrawable(R.drawable.hint_red)
            }
            ScanHint.ADJUST_ANGLE -> {
                capture_hint_text.text = resources.getString(R.string.adjust_angle)
                capture_hint_layout.background = resources.getDrawable(R.drawable.hint_red)
            }
            ScanHint.FIND_RECT -> {
                capture_hint_text.text = resources.getString(R.string.finding_rect)
                capture_hint_layout.background = resources.getDrawable(R.drawable.hint_blue)
            }
            ScanHint.CAPTURING_IMAGE -> {
                capture_hint_text.text = resources.getString(R.string.hold_still)
                capture_hint_layout.background = resources.getDrawable(R.drawable.hint_green)
            }
            ScanHint.NO_MESSAGE -> capture_hint_layout.setVisibility(View.GONE)
        }
    }

    /**
     * A document or card has been found, switch to the crop fragment
     * @param bitmap UNCROPPED bitmap
     */
    override fun onPictureClicked(bitmap: Bitmap?) {
        val rotateFragment = bitmap?.let { CropFragment.newInstance(bitmap) }
        val fm = activity.supportFragmentManager
        val transaction = fm.beginTransaction()
        transaction.replace(R.id.base_layout, rotateFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    // TODO
    companion object {
        private val TAG = ScanFragment::class.java.simpleName

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ScanFrament.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                ScanFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
