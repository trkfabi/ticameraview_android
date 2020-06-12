@file:Suppress("DEPRECATION")

package ti.cameraview.camera

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera as OldCamera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK as CC_LB
import android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT as CC_LF
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector.LENS_FACING_BACK as CS_LB
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT as CS_LF
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.TiApplication
import ti.cameraview.TicameraviewModule
import ti.cameraview.constant.Defaults.CAMERA_TYPE_UNKNOWN
import ti.cameraview.constant.Defaults.CAMERA_TYPE_BACK
import ti.cameraview.constant.Defaults.CAMERA_TYPE_FRONT
import ti.cameraview.constant.Defaults.CAMERA_TYPE_EXTERNAL
import ti.cameraview.constant.Methods.CameraList.CAMERA_ID
import ti.cameraview.constant.Methods.CameraList.CAMERA_TYPE
import ti.cameraview.helper.ResourceUtils
import java.util.Comparator
import kotlin.collections.ArrayList

object CameraUtils {
    const val LCAT = "CameraUtils"

    class CameraPropertyComparator: Comparator<HashMap<String, Any>> {
        override fun compare(map1: HashMap<String, Any>?, map2: HashMap<String, Any>?): Int {
            if (map1 == null || map2 == null){
                return 0;
            }

            return (map1["wide_focal"] as Float).compareTo(map2["wide_focal"] as Float)
        }
    }

    private fun addCameraListItem(list: ArrayList<KrollDict>, cameraId: Int, cameraType: String, focalLength: FloatArray? = null) {
        val cameraDict = KrollDict()
        cameraDict[CAMERA_ID] = cameraId
        cameraDict[CAMERA_TYPE] = cameraType

        // TODO enable the below codes when wide-angle camera support is available in CameraX
        // cameraDict[CAMERA_FOCAL_LENGTHS] = focalLength?.toTypedArray() ?: arrayOf(0.0f)
        // cameraDict[CAMERA_WIDEST_FOCAL_LENGTH] = focalLength?.min() ?: 0.0f

        list.add(cameraDict)
    }

    // TODO enable the below codes when wide-angle camera support is available in CameraX
    private fun getWideAngleCameraList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val context = TiApplication.getInstance().rootOrCurrentActivity
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                var cameraType = CAMERA_TYPE_UNKNOWN;

                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraType = CAMERA_TYPE_BACK
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraType = CAMERA_TYPE_FRONT
                } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    cameraType = CAMERA_TYPE_EXTERNAL
                }

                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun addCameraOnAboveLollipop(cameraManager: CameraManager, cameraList: ArrayList<KrollDict>, lensFacingType: Int, lensFacing: Int, cameraType: String) {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (facing == lensFacingType) {
                addCameraListItem(cameraList, lensFacing, cameraType)
                break
            }
        }
    }

    private fun addCameraBelowLollipop(cameraList: ArrayList<KrollDict>, totalCameras: Int, cameraFacing: Int, cameraType: String) {
        for (i in 0 until totalCameras) {
            var cameraInfo = OldCamera.CameraInfo();
            OldCamera.getCameraInfo(i, cameraInfo);

            if (cameraInfo.facing == cameraFacing) {
                addCameraListItem(cameraList, i, cameraType)
            }
        }
    }

    @JvmStatic fun isCameraSupported(): Boolean {
        return ResourceUtils.CONTEXT.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    @JvmStatic fun getCameraList(): ArrayList<KrollDict> {
        var cameraList = arrayListOf<KrollDict>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val context = TiApplication.getInstance().rootOrCurrentActivity
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

                // add another 1 front-camera if available
                addCameraOnAboveLollipop(cameraManager, cameraList, CC_LF, CS_LF, CAMERA_TYPE_FRONT)

                // add another 1 back-camera if available
                addCameraOnAboveLollipop(cameraManager, cameraList, CC_LB, CS_LB, CAMERA_TYPE_BACK)
            } else {
                val totalCameras = OldCamera.getNumberOfCameras()

                // add another 1 front-camera if available
                addCameraBelowLollipop(cameraList, totalCameras, OldCamera.CameraInfo.CAMERA_FACING_FRONT, CAMERA_TYPE_FRONT)

                // add another 1 back-camera if available
                addCameraBelowLollipop(cameraList, totalCameras, OldCamera.CameraInfo.CAMERA_FACING_BACK, CAMERA_TYPE_BACK)
            }
        } catch (exc: Exception) {
            Log.d(LCAT, "** Error in retrieving camera list: $exc")

            // fallback to definite single back-camera state
            addCameraListItem(cameraList, CameraSelector.LENS_FACING_BACK, CAMERA_TYPE_BACK)
        }

        return cameraList
    }

    // returns the widest available back-camera
    @JvmStatic fun getDefaultCameraId(): Int {
        val cameraList = TicameraviewModule.retrieveCameraList()

        // return default back-camera
        if (cameraList.isEmpty()) {
            return CameraSelector.LENS_FACING_BACK
        }

        // filter list by back-camera
        val backCameraList = cameraList.filter { it[CAMERA_TYPE] ==  CAMERA_TYPE_BACK}

        // return default back-camera
        if (backCameraList.isEmpty()) {
            return CameraSelector.LENS_FACING_BACK
        }

        // TODO enable the below codes when wide-angle camera support is available in CameraX
        // sort list by back-camera minimum focal lengths, first item will have the widest focal-length
        // val sortedBackCameraList = backCameraList.sortedWith(CameraPropertyComparator())

         val defaultCameraId = backCameraList[0][CAMERA_ID]
         Log.d(LCAT, "** getDefaultCameraId: ${defaultCameraId.toString().toInt()}")

        return defaultCameraId.toString().toInt()
    }

}