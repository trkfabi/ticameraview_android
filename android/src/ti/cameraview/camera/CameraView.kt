package ti.cameraview.camera

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.appcelerator.kroll.KrollDict
import org.appcelerator.kroll.KrollProxy
import org.appcelerator.titanium.TiApplication
import org.appcelerator.titanium.TiC
import org.appcelerator.titanium.proxy.TiViewProxy
import org.appcelerator.titanium.util.TiConvert
import org.appcelerator.titanium.util.TiFileHelper
import org.appcelerator.titanium.view.TiCompositeLayout
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement
import org.appcelerator.titanium.view.TiUIView
import ti.cameraview.constant.Defaults
import ti.cameraview.helper.PermissionHandler
import ti.modules.titanium.media.MediaModule
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraView(proxy: TiViewProxy) : TiUIView(proxy) {
    private var aspectRatio = Defaults.ASPECT_RATIO_4_3
    private var torchMode = Defaults.TORCH_MODE_OFF
    private var flashMode = Defaults.FLASH_MODE_AUTO
    private var scaleType = Defaults.SCALE_TYPE_FIT_CENTER
    private var focusMode = Defaults.FOCUS_MODE_AUTO
    private var resumeAutoFocus = Defaults.RESUME_AUTO_FOCUS_ON_AFTER_FOCUS_MODE_TAP
    private var autoFocusResumeTime = Defaults.RESUME_AUTO_FOCUS_TIME_AFTER_FOCUS_MODE_TAP

    private var rootView: TiCompositeLayout
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraView: PreviewView

    private val IsCameraViewAvailable get() = this::cameraView.isInitialized
    private val ThisActivity get() = proxy.activity
    private val MainExecutor get() = ContextCompat.getMainExecutor(ThisActivity)


    companion object {
        const val LCAT = "CameraView"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
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

        // hold a reference to the proxy's root view to add camera-preview later
        rootView = TiCompositeLayout(ThisActivity, arrangement)

        if (CameraFeatures.isCameraSupported() &&
                PermissionHandler.hasCameraPermission() &&
                PermissionHandler.hasStoragePermission()) {
            Log.d(LCAT, "****** creating camera-view 1…")
            createCameraPreview()
        }

        setNativeView(rootView)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    override fun processProperties(dict: KrollDict) {
        super.processProperties(dict)

        if (outerView == null) {
            return
        }
    }

    override fun propertyChanged(key: String, oldValue: Any?, newValue: Any?, proxy: KrollProxy) {
        if (outerView == null) {
            return
        }
    }

    fun createCameraPreview() {
        // avoid re-creating the camera-view
        if (IsCameraViewAvailable) {
            Log.d(LCAT, "****** camera-view already added…")
            return
        }

        Log.d(LCAT, "****** creating camera-view 2 …")

        val layoutParams = TiCompositeLayout.LayoutParams()
        layoutParams.autoFillsHeight = true
        layoutParams.autoFillsWidth = true
        cameraView = PreviewView(ThisActivity)
        rootView.addView(cameraView, layoutParams)
        Log.d(LCAT, "****** camera-view added…")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(TiApplication.getAppCurrentActivity())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

                preview = Preview.Builder()
                        .setTargetAspectRatio(aspectRatio)
                        .build()

                preview?.setSurfaceProvider(cameraView.createSurfaceProvider())

                imageCapture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setTargetAspectRatio(aspectRatio)
                        .build()

//                // sets the image output dimensions ratio { DOES NOT WORK PROPERLY IN ASPECT RATIO 16_9 }
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    imageCapture?.setCropAspectRatio(Rational(1, 1))
//                }

                camera = cameraProvider.bindToLifecycle(TiApplication.getAppCurrentActivity() as LifecycleOwner, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(LCAT, "Use case binding failed", exc)
            }

        }, MainExecutor)

        Log.d(LCAT, "****** startCamera…")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateAutoFocusMode() {
        if (focusMode == Defaults.FOCUS_MODE_TAP) {
            cameraView.setOnTouchListener { view, event ->
                when(event.action) {
                    MotionEvent.ACTION_UP -> view.performClick()
                    MotionEvent.ACTION_DOWN -> CameraFeatures.changeFocusMode(camera, cameraView, event, resumeAutoFocus, autoFocusResumeTime)
                }
                return@setOnTouchListener true
            }

        } else if (focusMode == Defaults.FOCUS_MODE_AUTO) {
            // resets the auto-focus to continous mode
            camera?.cameraControl?.cancelFocusAndMetering()
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = File(ThisActivity.cacheDir, SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, MainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(LCAT, "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                val msg = "Photo saved at `CameraX` folder"
                Toast.makeText(ThisActivity, msg, Toast.LENGTH_SHORT).show()
                Log.d(LCAT, msg)
            }
        })
    }

    private fun saveImageAsBitmap() {
        imageCapture?.takePicture(MainExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    fun onError(error: ImageCapture.ImageCaptureError, message: String, exc: Throwable?) {
                        Toast.makeText(ThisActivity, "Error in image capture", Toast.LENGTH_SHORT).show()
                    }

                    @SuppressLint("UnsafeExperimentalUsageError")
                    fun onCaptureSuccess(imageProxy: ImageProxy, rotationDegrees: Int) {
                        imageProxy.image?.let {
                            val bitmap = rotateImage(imageToBitmap(it), rotationDegrees.toFloat())
                        }
                    }
                }
        )
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    @SuppressLint("WrongConstant")
    private fun toggleFlashlight() {
        if (camera?.cameraInfo?.hasFlashUnit() == false) return

        when (imageCapture?.flashMode) {
            // TODO()
        }
    }

    private fun toggleTorch() {
        if (camera?.cameraInfo?.hasFlashUnit() == false) return

        val torchState = camera?.cameraInfo?.torchState
        camera?.cameraControl?.enableTorch(torchState?.value == TorchState.OFF)
    }

    private fun getFile(isPublic: Boolean): File? {
        var file: File? = null
        val fileType = Environment.DIRECTORY_PICTURES
        val dir = if (isPublic) Environment.getExternalStoragePublicDirectory(fileType) else TiApplication.getInstance().getExternalFilesDir(fileType)
        val appDir = File(dir, TiApplication.getInstance().appInfo.name);

        if (!appDir.exists() && !appDir.mkdirs()) {
            Log.e("TiMedia", "Failed to create external storage directory.");
        } else {
            file = TiFileHelper.getInstance().getTempFile(appDir, Defaults.IMAGE_EXTENSION, !isPublic);
        }

        return file;
    }
}
