package ti.cameraview.camera

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import ti.cameraview.helper.ResourceUtils
import java.util.*
import java.util.concurrent.TimeUnit

object CameraFeatures {
    private const val LCAT = "CameraFeature"

    @JvmStatic fun isCameraSupported(): Boolean {
        return ResourceUtils.CONTEXT.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    @JvmStatic fun changeFocusMode(camera: Camera?,
                                   cameraView: PreviewView,
                                   event: MotionEvent,
                                   resumeAutoFocus: Boolean,
                                   autoFocusResumeTime: Int) {
        cameraView.doOnLayout {
            val meteringFactory = SurfaceOrientedMeteringPointFactory(it.width.toFloat(), it.height.toFloat())
            val meteringPoint = meteringFactory.createPoint(event.x, event.y);
            var action: FocusMeteringAction;

            if (resumeAutoFocus) {
                action = FocusMeteringAction.Builder(meteringPoint)
                        .setAutoCancelDuration(autoFocusResumeTime.toLong(), TimeUnit.SECONDS)
                        .build()
            } else {
                action = FocusMeteringAction.Builder(meteringPoint)
                        .disableAutoCancel()
                        .build()
            }

            camera?.cameraControl?.startFocusAndMetering(action)
        }
    }


    @JvmStatic fun getCameraFeature(context: Context): String {
        var msg = ""

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return msg
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in cameraManager.cameraIdList) {
                msg += "\n---------------------- Camera ID = $cameraId ------------------------\n"
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                // Examine the LENS_FACING characteristic
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                msg += if (facing == null) {
                    "Facing: NULL"
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    "Facing: BACK"
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    "Facing: FRONT"
                } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    "Facing: EXTERNAL"
                } else {
                    "Facing: UNKNOWN"
                }

                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                msg += "\nAvailable focal lengths : " + Arrays.toString(focalLengths)

                val logicalCapabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                msg += "\nAvailable logicalCapabilities : " + Arrays.toString(logicalCapabilities)

                var idText = ""
                val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    characteristics.physicalCameraIds
                } else {
                    TODO("VERSION.SDK_INT < P")
                }

                if (physicalIds.size == 0) {
                    idText = "No physical camera available"
                } else {
                    for (id in physicalIds) {
                        idText += "$id, "
                    }
                }

                msg += "\nPhysical cameras : $idText"
                msg += "\n\n"
            }
        } catch (e: CameraAccessException) {
            msg += "\nCameraAccessException: " + e.message
        } catch (e: NullPointerException) {
            msg += "\nNullPointerException: " + e.message
        }
        return msg
    }
}