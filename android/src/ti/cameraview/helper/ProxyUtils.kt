package ti.cameraview.helper

import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.proxy.TiViewProxy

class ProxyUtils {
    fun fireMoveEvent(tiViewProxy: TiViewProxy, imagePath: String) {
        val eventData = KrollDict()
        eventData[Constants.Events.Image.PROPERTY_IMAGE_PATH] = imagePath
        tiViewProxy.fireEvent(Constants.Events.Image.NAME, eventData)
    }
}