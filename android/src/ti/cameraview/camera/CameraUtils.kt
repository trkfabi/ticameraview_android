@file:Suppress("DEPRECATION")

package ti.cameraview.camera

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera as OldCamera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.TiApplication
import ti.cameraview.constant.Defaults
import ti.cameraview.constant.Methods
import ti.cameraview.helper.ResourceUtils
import java.util.*
import kotlin.collections.ArrayList

object CameraUtils {
    private fun addCameraListItem(list: ArrayList<Any>, cameraId: Int, cameraType: String, focalLength: Array<Any>? = null) {
        val cameraDict = KrollDict()
        cameraDict[Methods.CameraList.CAMERA_ID] = cameraId
        cameraDict[Methods.CameraList.CAMERA_TYPE] = cameraType
        cameraDict[Methods.CameraList.CAMERA_FOCAL_LENGTH] = focalLength ?: arrayOf("0")

        list.add(cameraDict)
    }

    @JvmStatic fun isCameraSupported(): Boolean {
        return ResourceUtils.CONTEXT.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    @JvmStatic fun getCameraList(): ArrayList<Any> {
        var cameraList = arrayListOf<Any>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val context = TiApplication.getInstance().rootOrCurrentActivity
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                var cameraType = Defaults.CAMERA_TYPE_UNKNOWN;

                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraType = Defaults.CAMERA_TYPE_BACK
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraType = Defaults.CAMERA_TYPE_FRONT
                } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    cameraType = Defaults.CAMERA_TYPE_EXTERNAL
                }

                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalLengthsModified = focalLengths?.map {
                    "%.2f".format(it)
                }

                addCameraListItem(cameraList, cameraId.toInt(), cameraType, focalLengthsModified?.toTypedArray())
            }
        } else {
            addCameraListItem(cameraList, CameraSelector.LENS_FACING_BACK, Defaults.CAMERA_TYPE_BACK)

            val totalCameras = OldCamera.getNumberOfCameras()

            for (i in 0 until totalCameras) {
                var cameraInfo = OldCamera.CameraInfo();
                OldCamera.getCameraInfo(i, cameraInfo);

                if (cameraInfo.facing == OldCamera.CameraInfo.CAMERA_FACING_FRONT) {
                    addCameraListItem(cameraList, i, Defaults.CAMERA_TYPE_FRONT)
                }
            }
        }

        return cameraList
    }
}