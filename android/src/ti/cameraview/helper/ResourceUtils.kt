package ti.cameraview.helper

import android.content.Context
import androidx.camera.view.PreviewView
import org.appcelerator.titanium.TiApplication
import org.appcelerator.titanium.util.TiRHelper
import ti.cameraview.constant.Defaults

object ResourceUtils {
    @JvmStatic fun getContext(): Context {
        return TiApplication.getInstance().rootOrCurrentActivity
    }

    @JvmStatic fun getString(key: String): String {
        return try {
            getContext().resources.getString(TiRHelper.getResource("string.$key"))
        } catch (exc: Exception) {
            key
        }
    }

    @JvmStatic fun getScaleType(scaleType: Int): PreviewView.ScaleType {
        return when(scaleType) {
            Defaults.SCALE_TYPE_FILL_CENTER -> PreviewView.ScaleType.FILL_CENTER
            Defaults.SCALE_TYPE_FIT_CENTER -> PreviewView.ScaleType.FIT_CENTER
            else -> PreviewView.ScaleType.FILL_CENTER
        }
    }
}