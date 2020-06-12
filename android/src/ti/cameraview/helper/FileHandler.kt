package ti.cameraview.helper

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import ti.cameraview.TicameraviewModule

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

    @SuppressLint("UnsafeExperimentalUsageError")
    @JvmStatic fun generateBitmap(imageProxy: ImageProxy): Bitmap? {
        val bitmap = imageToBitmap(imageProxy.image!!)
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return rotateImage(bitmap, rotationDegrees.toFloat())
    }
}