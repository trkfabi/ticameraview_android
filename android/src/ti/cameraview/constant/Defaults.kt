/*
 * Default values to be applied on camera-view
 * The first defined property in groups is the default one
 */

package ti.cameraview.constant

import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.view.PreviewView
import org.appcelerator.kroll.annotations.Kroll

object Defaults {
    const val IMAGE_EXTENSION = ".jpg"

    const val TORCH_MODE_OFF = TorchState.OFF
    const val TORCH_MODE_ON = TorchState.ON

    const val ASPECT_RATIO_4_3 = AspectRatio.RATIO_4_3
    const val ASPECT_RATIO_16_9 = AspectRatio.RATIO_16_9

    const val FLASH_MODE_AUTO = ImageCapture.FLASH_MODE_AUTO
    const val FLASH_MODE_ON = ImageCapture.FLASH_MODE_ON
    const val FLASH_MODE_OFF = ImageCapture.FLASH_MODE_OFF

    const val SCALE_TYPE_FIT_CENTER = 4     // PreviewView.ScaleType.FIT_CENTER
    const val SCALE_TYPE_FILL_CENTER = 1    // PreviewView.ScaleType.FILL_CENTER

    const val FOCUS_MODE_AUTO = 0           // auto-focus
    const val FOCUS_MODE_TAP = 1            // tap-to-focus

    const val RESUME_AUTO_FOCUS_AFTER_FOCUS_MODE_TAP = true

    const val RESUME_AUTO_FOCUS_TIME_AFTER_FOCUS_MODE_TAP = 5    // in seconds

    const val CAMERA_TYPE_BACK = "CAMERA_TYPE_BACK"
    const val CAMERA_TYPE_FRONT = "CAMERA_TYPE_FRONT"
    const val CAMERA_TYPE_UNKNOWN = "CAMERA_TYPE_UNKNOWN"
    const val CAMERA_TYPE_EXTERNAL = "CAMERA_TYPE_EXTERNAL"
}
