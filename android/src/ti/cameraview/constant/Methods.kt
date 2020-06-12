/*
 * Module methods and their arguments list
 */

package ti.cameraview.constant

import android.graphics.Bitmap
import android.util.Log
import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.TiBlob
import ti.cameraview.TicameraviewModule
import ti.cameraview.helper.ResourceUtils

object Methods {
    object CameraList {
        const val CAMERA_ID = "cameraId"
        const val CAMERA_TYPE = "cameraType"

        // TODO enable the below codes when wide-angle camera support is available in CameraX
        const val CAMERA_FOCAL_LENGTHS = "cameraFocalLengths"
        const val CAMERA_WIDEST_FOCAL_LENGTH = "cameraWidestFocalLength"
    }

    object CapturePhoto {
        const val PROPERTY_CALLBACK = "callback"
        private const val RESULT_IMAGE = "image"
        private const val RESULT_SUCCESS = "success"
        private const val RESULT_MESSAGE = "message"

        @JvmStatic fun createResult(image: Bitmap?, success: Boolean, messageId: String? = null): KrollDict {
            return KrollDict().apply {
                this[RESULT_IMAGE] = image?.let { TiBlob.blobFromImage(image) } ?: null
                this[RESULT_SUCCESS] = success
                this[RESULT_MESSAGE] = messageId?.let { ResourceUtils.getString(it) } ?: ""
            }
        }
    }
}
