package ti.cameraview.helper

import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.proxy.TiViewProxy
import ti.cameraview.constant.Events

class ProxyUtils {
    fun fireMoveEvent(tiViewProxy: TiViewProxy, imagePath: String) {
        val eventData = KrollDict()
        eventData[Events.Image.PROPERTY_IMAGE_PATH] = imagePath
        tiViewProxy.fireEvent(Events.Image.NAME, eventData)
    }
}