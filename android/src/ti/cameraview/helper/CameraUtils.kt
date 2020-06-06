package ti.cameraview.helper

import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.proxy.TiViewProxy

class CameraUtils {
    fun fireMoveEvent(tiViewProxy: TiViewProxy, imagePath: String) {
        val eventData = KrollDict()
        eventData[Defaults.Events.Image.EVENT_PROPERTY_IMAGE_PATH] = imagePath
        tiViewProxy.fireEvent(Defaults.Events.Image.EVENT_NAME, eventData)
    }
}