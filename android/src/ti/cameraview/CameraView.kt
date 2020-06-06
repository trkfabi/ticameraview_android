package ti.cameraview

import android.util.Log
import org.appcelerator.kroll.KrollDict
import org.appcelerator.kroll.KrollProxy
import org.appcelerator.titanium.TiC
import org.appcelerator.titanium.proxy.TiViewProxy
import org.appcelerator.titanium.util.TiConvert
import org.appcelerator.titanium.view.TiCompositeLayout
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement
import org.appcelerator.titanium.view.TiUIView

class CameraView(proxy: TiViewProxy) : TiUIView(proxy) {
    companion object {
        private const val LCAT = "CameraView"
    }

    init {
        var arrangement = LayoutArrangement.DEFAULT
        if (proxy.hasProperty(TiC.PROPERTY_LAYOUT)) {
            val layoutProperty = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_LAYOUT))
            if (layoutProperty == TiC.LAYOUT_HORIZONTAL) {
                arrangement = LayoutArrangement.HORIZONTAL
            } else if (layoutProperty == TiC.LAYOUT_VERTICAL) {
                arrangement = LayoutArrangement.VERTICAL
            }
        }
        setNativeView(TiCompositeLayout(proxy.activity, arrangement))
    }

    override fun processProperties(dict: KrollDict) {
        super.processProperties(dict)
        if (outerView == null) {
            return
        }
        if (dict.containsKey("color")) {
            Log.d(LCAT, "processProperties: color = " + dict.getString("color"))
            setColor(TiConvert.toColor(dict, "color"))
        }
    }

    override fun propertyChanged(key: String, oldValue: Any, newValue: Any, proxy: KrollProxy) {
        if (outerView == null) {
            return
        }
        if (key == "color") {
            Log.d(LCAT, "propertyChanged: color : from = $oldValue : to = $newValue")
            setColor(TiConvert.toColor(newValue.toString()))
        }
    }

    private fun setColor(color: Int) {
        outerView.setBackgroundColor(color)
    }
}