/*
 * Default values to be applied on camera-view
 */

package ti.cameraview.constant

import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView

object Defaults {
    const val IMAGE_EXTENSION = ".jpg"

    const val ASPECT_RATIO_4_3 = AspectRatio.RATIO_4_3
    const val ASPECT_RATIO_16_9 = AspectRatio.RATIO_16_9

    const val TORCH_MODE_ON = true
    const val TORCH_MODE_OFF = false

    const val FLASH_MODE_AUTO = ImageCapture.FLASH_MODE_AUTO
    const val FLASH_MODE_ON = ImageCapture.FLASH_MODE_ON
    const val FLASH_MODE_OFF = ImageCapture.FLASH_MODE_OFF

    @JvmField val SCALE_TYPE_FIT_CENTER = PreviewView.ScaleType.FIT_CENTER
    @JvmField val SCALE_TYPE_FILL_CENTER = PreviewView.ScaleType.FILL_CENTER
    @JvmField val SCALE_TYPE_FIT_START = PreviewView.ScaleType.FIT_START
    @JvmField val SCALE_TYPE_FILL_START = PreviewView.ScaleType.FILL_START
    @JvmField val SCALE_TYPE_FIT_END = PreviewView.ScaleType.FIT_END
    @JvmField val SCALE_TYPE_FILL_END = PreviewView.ScaleType.FILL_END

    const val FOCUS_MODE_AUTO = 0     // auto-focus
    const val FOCUS_MODE_TAP = 1      // tap-to-focus

    const val RESUME_AUTO_FOCUS_ON_AFTER_FOCUS_MODE_TAP = true
    const val RESUME_AUTO_FOCUS_OFF_AFTER_FOCUS_MODE_TAP = false
    const val RESUME_AUTO_FOCUS_TIME_AFTER_FOCUS_MODE_TAP = 5    // in seconds
}