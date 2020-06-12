/*
 * Module events and their properties
 */

package ti.cameraview.constant

import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.proxy.TiViewProxy
import ti.cameraview.helper.ResourceUtils

object Events {
    object Image {
        const val NAME = "image"
        const val PROPERTY_IMAGE_PATH = "imagePath"
    }

    object CameraReady {
        const val NAME = "cameraReady"
        const val PROPERTY_SUCCESS = "success"
        const val PROPERTY_MESSAGE = "message"
    }

    @JvmStatic fun fireCameraReadyEvent(proxy: TiViewProxy, isSuccess: Boolean, messageId: String? = null, message: String = "") {
        val eventData = KrollDict()
        eventData[Events.CameraReady.PROPERTY_SUCCESS] = isSuccess
        eventData[Events.CameraReady.PROPERTY_MESSAGE] = messageId?.let { ResourceUtils.getString(it) } ?: message
        proxy.fireEvent(Events.CameraReady.NAME, eventData)
    }

    @JvmStatic fun fireImageCaptureEvent(proxy: TiViewProxy, imagePath: String) {
        val eventData = KrollDict()
        eventData[Events.Image.PROPERTY_IMAGE_PATH] = imagePath
        proxy.fireEvent(Events.Image.NAME, eventData)
    }
}