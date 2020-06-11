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
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
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
import ti.cameraview.constant.Defaults.ASPECT_RATIO_4_3
import ti.cameraview.constant.Defaults.FLASH_MODE_AUTO
import ti.cameraview.constant.Defaults.FOCUS_MODE_AUTO
import ti.cameraview.constant.Defaults.FOCUS_MODE_TAP
import ti.cameraview.constant.Defaults.RESUME_AUTO_FOCUS_AFTER_FOCUS_MODE_TAP
import ti.cameraview.constant.Defaults.RESUME_AUTO_FOCUS_TIME_AFTER_FOCUS_MODE_TAP
import ti.cameraview.constant.Defaults.SCALE_TYPE_FIT_CENTER
import ti.cameraview.constant.Defaults.TORCH_MODE_OFF
import ti.cameraview.constant.Properties.TORCH_MODE
import ti.cameraview.constant.Properties.FLASH_MODE
import ti.cameraview.constant.Properties.ASPECT_RATIO
import ti.cameraview.constant.Properties.SCALE_TYPE
import ti.cameraview.constant.Properties.FOCUS_MODE
import ti.cameraview.constant.Properties.RESUME_AUTO_FOCUS
import ti.cameraview.constant.Properties.AUTO_FOCUS_RESUME_TIME
import ti.cameraview.helper.PermissionHandler
import ti.cameraview.helper.ResourceUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class CameraView(proxy: TiViewProxy) : TiUIView(proxy) {
    private var rootView: TiCompositeLayout
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraView: PreviewView

    private val ThisActivity get() = proxy.activity
    private val MainExecutor get() = ContextCompat.getMainExecutor(ThisActivity)


    companion object {
        const val LCAT = "CameraView"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }


    inner class Utils {
        fun getBoolean(key: String, defaultValue: Any): Boolean {
            return if (proxy.hasPropertyAndNotNull(key)) {
                TiConvert.toBoolean(proxy.getProperty(key))
            } else {
                TiConvert.toBoolean(defaultValue)
            }
        }

        fun getInt(key: String, defaultValue: Any): Int {
            return if (proxy.hasPropertyAndNotNull(key)) {
                TiConvert.toInt(proxy.getProperty(key))
            } else {
                TiConvert.toInt(defaultValue)
            }
        }

        fun getColor(key: String): Int {
            return if (proxy.hasPropertyAndNotNull(key)) {
                TiConvert.toColor(proxy.getProperty(key) as String?)
            } else {
                TiConvert.toColor("white")
            }
        }
    }


    init {
        // sanity check for `lifecycleContainer` property to control the activity lifecycle
        if (!proxy.properties.containsKeyAndNotNull(TiC.PROPERTY_LIFECYCLE_CONTAINER)) {
            Log.d(LCAT, "`lifecycleContainer` property missing, some features like `torch` may not work properly")
        }

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

    override fun processProperties(d: KrollDict?) {
        super.processProperties(d)
    }

    override fun propertyChanged(key: String, oldValue: Any?, newValue: Any?, proxy: KrollProxy) {
        super.propertyChanged(key, oldValue, newValue, proxy)

        if (outerView == null) {
            Log.d(LCAT, "camera-view container not created")
            return
        }

        Log.d(LCAT, "propertyChanged : $key : from ${oldValue ?: "null"} to $newValue")

        when(key) {
            TORCH_MODE -> handleTorch()
            FLASH_MODE -> handleFlash()
            ASPECT_RATIO -> handleAspectRatio()
            SCALE_TYPE -> handleScaleType()
            FOCUS_MODE -> {
                when(Utils().getInt(FOCUS_MODE, FOCUS_MODE_AUTO)) {
                    FOCUS_MODE_AUTO -> camera?.cameraControl?.cancelFocusAndMetering()
                    FOCUS_MODE_TAP -> startFocus(cameraView.width.toFloat()/2, cameraView.height.toFloat()/2)
                }
            }
        }
    }

    override fun release() {
        rootView.removeAllViews()

        if (preview != null) preview = null
        if (imageCapture != null) imageCapture = null
        if (camera != null) camera = null

        super.release()
    }

    // check whether camera is ready to use
    fun isCameraReady(): Boolean {
        return this::cameraView.isInitialized
    }


    // {START} -> module-property handlers
    fun handleTorch() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch( Utils().getBoolean(TORCH_MODE, TORCH_MODE_OFF) )
        }
    }

    private fun handleFlash() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            imageCapture?.flashMode = Utils().getInt(FLASH_MODE, FLASH_MODE_AUTO)
        }
    }

    private fun handleAspectRatio() {
        Log.d(LCAT, "** handleAspectRatio")
        createCameraPreview(rebindView = true)
    }

    private fun handleScaleType() {
        cameraView?.scaleType = ResourceUtils.getScaleType(Utils().getInt(SCALE_TYPE, SCALE_TYPE_FIT_CENTER))
    }

    // start focusing on the given co-ordinates in TAP_TO_FOCUS mode
    private fun startFocus(x: Float, y: Float) {
        if (Utils().getInt(FOCUS_MODE, FOCUS_MODE_AUTO) != FOCUS_MODE_TAP) {
            camera?.cameraControl?.cancelFocusAndMetering()
            return
        }

        val meteringFactory = cameraView.createMeteringPointFactory(cameraSelector)
        val meteringPoint = meteringFactory.createPoint(x, y)

        val action = if (Utils().getBoolean(RESUME_AUTO_FOCUS, RESUME_AUTO_FOCUS_AFTER_FOCUS_MODE_TAP)) {
            FocusMeteringAction.Builder(meteringPoint)
                    .setAutoCancelDuration( Utils().getInt(AUTO_FOCUS_RESUME_TIME, RESUME_AUTO_FOCUS_TIME_AFTER_FOCUS_MODE_TAP).toLong(), TimeUnit.SECONDS)
        } else {
            FocusMeteringAction.Builder(meteringPoint)
                    .disableAutoCancel()
        }

        camera?.cameraControl?.startFocusAndMetering(action.build())
    }
    // {END} -> module-property handlers


    @SuppressLint("ClickableViewAccessibility")
    fun createCameraPreview(rebindView: Boolean = false) {
        if (!rebindView) {
            // make sure all existing child views are removed before adding the camera-view
            rootView.removeAllViews()

            val layoutParams = TiCompositeLayout.LayoutParams()
            layoutParams.autoFillsHeight = true
            layoutParams.autoFillsWidth = true

            cameraView = PreviewView(ThisActivity)
            cameraView.setBackgroundColor(Utils().getColor(TiC.PROPERTY_BACKGROUND_COLOR))
            handleScaleType()

            // apply onTouch listener to listen for TAP_TO_FOCUS actions
            cameraView.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_DOWN) startFocus(event.x, event.y)
                true
            }

            rootView.addView(cameraView, layoutParams)
            Log.d(LCAT, "****** camera-view created…")
        } else {
            Log.d(LCAT, "****** camera-view already added…")
        }

        ProcessCameraProvider.getInstance(ThisActivity).apply {
            addListener(Runnable {
                val cameraProvider: ProcessCameraProvider = get()

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

                    preview = Preview.Builder()
                            .setTargetAspectRatio(Utils().getInt(ASPECT_RATIO, ASPECT_RATIO_4_3))
                            .build()

                    preview?.setSurfaceProvider(cameraView.createSurfaceProvider())

                    imageCapture = ImageCapture.Builder()
                            .setFlashMode(Utils().getInt(FLASH_MODE, FLASH_MODE_AUTO))
                            .setTargetAspectRatio(Utils().getInt(ASPECT_RATIO, ASPECT_RATIO_4_3))
                            .build()

    //                // sets the image output dimensions ratio { DOES NOT WORK PROPERLY IN ASPECT RATIO 16_9 }
    //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    //                    imageCapture?.setCropAspectRatio(Rational(1, 1))
    //                }

                    camera = cameraProvider.bindToLifecycle(ThisActivity as LifecycleOwner, cameraSelector, preview, imageCapture)

                    handleTorch()

                } catch(exc: Exception) {
                    Log.e(LCAT, "Use case binding failed", exc)
                }

            }, MainExecutor)
        }

        Log.d(LCAT, "****** startCamera…")
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
