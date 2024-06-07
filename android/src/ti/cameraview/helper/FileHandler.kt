package ti.cameraview.helper

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import org.appcelerator.titanium.TiApplication
import org.appcelerator.titanium.TiBlob
import org.appcelerator.titanium.TiFileProxy
import org.appcelerator.titanium.io.TiFileFactory
import org.appcelerator.titanium.io.TiFileProvider
import org.appcelerator.titanium.util.TiFileHelper
import ti.cameraview.TicameraviewModule
import java.io.File
import java.io.IOException


object FileHandler {
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotatedBitmap= Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        // release the source bitmap
        if (!source.isRecycled) {
            Log.d(TicameraviewModule.LCAT, "source bitmap recycled")
        }

        return rotatedBitmap
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    @JvmStatic fun generateBitmap(imageProxy: ImageProxy): Bitmap? {
        val bitmap = imageToBitmap(imageProxy.image!!)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return rotateImage(bitmap, rotationDegrees.toFloat())
    }

    @JvmStatic fun createExternalStorageFile(fileExtension: String = "jpg"): File? {
        val dir = when (fileExtension) {
            "jpg" -> TiApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            else -> TiApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        } ?: return null
        var appDir = File(dir, TiApplication.getInstance().appInfo.name)

        if (!appDir.exists() && !appDir.mkdirs()) {
                appDir = TiApplication.getInstance().cacheDir
        }

        try {
            return TiFileHelper.getInstance().getTempFile(".$fileExtension", true)
        } catch (exc: IOException) {
            Log.e("", "Failed to create file: " + exc.message)
        }

        return null
    }

    @JvmStatic fun generateFileProxy(file: File): TiFileProxy {
        return TiFileProxy(TiFileFactory.createTitaniumFile(file.absolutePath, false))
    }

}