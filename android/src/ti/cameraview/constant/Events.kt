/*
 * Module events and their properties
 */

package ti.cameraview.constant

import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.proxy.TiViewProxy
import ti.cameraview.helper.ResourceUtils

object Events {
    object CameraReady {
        const val NAME = "cameraready"
        const val PROPERTY_SUCCESS = "success"
        const val PROPERTY_MESSAGE = "message"
    }

    @JvmStatic fun fireCameraReadyEvent(proxy: TiViewProxy, isSuccess: Boolean, messageId: String? = null, message: String = "") {
        val eventData = KrollDict()
        eventData[CameraReady.PROPERTY_SUCCESS] = isSuccess
        eventData[CameraReady.PROPERTY_MESSAGE] = messageId?.let { ResourceUtils.getString(it) } ?: message
        proxy.fireEvent(CameraReady.NAME, eventData)
    }
}