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
    @JvmField val CONTEXT: Context = TiApplication.getInstance().applicationContext

    fun convertDpToPx(dp: Float): Float {
        return dp * CONTEXT.resources?.displayMetrics?.density!!
    }

    fun convertPxToDp(px: Float): Float {
        return px / CONTEXT.resources?.displayMetrics?.density!!
    }

    fun getR(path: String?): Int {
        return try {
            TiRHelper.getResource(path)
        } catch (exc: Exception) {
            -1
        }
    }

    fun getObjectOption(options: KrollDict, key: String): Any? {
        return if (options.containsKeyAndNotNull(key)) options[key] else null
    }

    fun getStringOption(options: KrollDict, key: String): String? {
        if (options.containsKeyAndNotNull(key)) {
            return TiConvert.toString(options[key], "").trim()
        }

        return null
    }

    fun getIntOption(options: KrollDict, key: String): Int? {
        if (options.containsKeyAndNotNull(key)) {
            return TiConvert.toInt(options[key], -1)
        }

        return null
    }

    fun getBoolOption(options: KrollDict, key: String): Boolean? {
        if (options.containsKeyAndNotNull(key)) {
            return TiConvert.toBoolean(options[key], false)
        }

        return null
    }

    fun getBoolOption(options: KrollDict, key: String, defaultValue: Boolean): Boolean {
        if (options.containsKeyAndNotNull(key)) {
            return TiConvert.toBoolean(options[key], defaultValue)
        }

        return defaultValue
    }

    fun getArrayOption(options: KrollDict, key: String?): Array<String>? {
        return if (options.containsKeyAndNotNull(key)) options.getStringArray(key) else null
    }

    fun getResValueAsInt(intResName: String): Int {
        val resId = getR("dimen.$intResName")
        return if (resId == -1) {
            0
        } else {
            val context = TiApplication.getInstance().applicationContext
            context.resources.getInteger(resId)
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