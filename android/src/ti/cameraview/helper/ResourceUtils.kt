package ti.cameraview.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.camera.view.PreviewView
import org.appcelerator.kroll.KrollDict
import org.appcelerator.titanium.TiApplication
import org.appcelerator.titanium.proxy.TiViewProxy
import org.appcelerator.titanium.util.TiConvert
import org.appcelerator.titanium.util.TiRHelper
import org.appcelerator.titanium.view.TiDrawableReference
import ti.cameraview.TicameraviewModule
import ti.cameraview.constant.Defaults
import ti.modules.titanium.filesystem.FileProxy

object ResourceUtils {
    @JvmField val CONTEXT: Context? = TiApplication.getInstance().applicationContext

    @JvmStatic fun getString(key: String): String {
        return try {
            CONTEXT?.resources?.getString(TiRHelper.getResource("string.$key")) ?: ""
        } catch (exc: Exception) {
            key
        }
    }

    fun getImageDrawable(anyObject: Any, proxy: TiViewProxy): Drawable? {
        if (anyObject == null) {
            return null
        }

        val imageDrawableReference: TiDrawableReference? = if (anyObject is FileProxy) {
            TiDrawableReference.fromFile(proxy.activity, anyObject.baseFile)
        } else if (anyObject is String) {
            TiDrawableReference.fromUrl(proxy, anyObject as String?)
        } else {
            TiDrawableReference.fromObject(proxy, anyObject)
        }

        return imageDrawableReference?.densityScaledDrawable
    }

    @JvmStatic fun getScaleType(scaleType: Int): PreviewView.ScaleType {
        return when(scaleType) {
            Defaults.SCALE_TYPE_FILL_CENTER -> PreviewView.ScaleType.FILL_CENTER
            Defaults.SCALE_TYPE_FIT_CENTER -> PreviewView.ScaleType.FIT_CENTER
            else -> PreviewView.ScaleType.FILL_CENTER
        }
    }
}