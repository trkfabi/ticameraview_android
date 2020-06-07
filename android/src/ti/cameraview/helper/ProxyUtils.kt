package ti.cameraview.helper

import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.proxy.TiViewProxy

class ProxyUtils {
    fun fireMoveEvent(tiViewProxy: TiViewProxy, imagePath: String) {
        val eventData = KrollDict()
        eventData[Defaults.Events.Image.PROPERTY_IMAGE_PATH] = imagePath
        tiViewProxy.fireEvent(Defaults.Events.Image.NAME, eventData)
    }
}