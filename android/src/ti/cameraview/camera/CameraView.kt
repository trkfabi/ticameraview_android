package ti.cameraview.camera

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.MotionEvent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.appcelerator.kroll.KrollDict
import org.appcelerator.kroll.KrollFunction
import org.appcelerator.kroll.KrollProxy
import org.appcelerator.titanium.TiC
import org.appcelerator.titanium.proxy.TiViewProxy
import org.appcelerator.titanium.util.TiConvert
import org.appcelerator.titanium.view.TiCompositeLayout
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement
import org.appcelerator.titanium.view.TiUIView
import ti.cameraview.constant.Defaults.ASPECT_RATIO_4_3
import ti.cameraview.constant.Defaults.FLASH_MODE_AUTO
import ti.cameraview.constant.Defaults.FOCUS_MODE_AUTO
import ti.cameraview.constant.Defaults.FOCUS_MODE_TAP
import ti.cameraview.constant.Defaults.IMAGE_QUALITY_NORMAL
import ti.cameraview.constant.Defaults.RESUME_AUTO_FOCUS_AFTER_FOCUS_MODE_TAP
import ti.cameraview.constant.Defaults.RESUME_AUTO_FOCUS_TIME_AFTER_FOCUS_MODE_TAP
import ti.cameraview.constant.Defaults.SCALE_TYPE_FIT_CENTER
import ti.cameraview.constant.Defaults.TORCH_MODE_OFF
import ti.cameraview.constant.Events
import ti.cameraview.constant.Methods
import ti.cameraview.constant.Properties.ASPECT_RATIO
import ti.cameraview.constant.Properties.AUTO_FOCUS_RESUME_TIME
import ti.cameraview.constant.Properties.CAMERA_ID
import ti.cameraview.constant.Properties.FLASH_MODE
import ti.cameraview.constant.Properties.FOCUS_MODE
import ti.cameraview.constant.Properties.IMAGE_QUALITY
import ti.cameraview.constant.Properties.RESUME_AUTO_FOCUS
import ti.cameraview.constant.Properties.SCALE_TYPE
import ti.cameraview.constant.Properties.TORCH_MODE
import ti.cameraview.helper.FileHandler.generateBitmap
import ti.cameraview.helper.PermissionHandler
import ti.cameraview.helper.ResourceUtils
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
            if (proxy.hasPropertyAndNotNull(key)) {
                val a = TiConvert.toInt(proxy.getProperty(key))
                return a
            } else {
                return TiConvert.toInt(defaultValue)
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

        if (CameraUtils.isCameraSupported() &&
                PermissionHandler.hasCameraPermission() &&
                PermissionHandler.hasStoragePermission()) {
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
            return
        }

        // check if the property has been really changed to avoid rebinding camera-view and execute other features
        if (oldValue == newValue) {
            return
        }

        when(key) {
            ASPECT_RATIO, CAMERA_ID, IMAGE_QUALITY -> createCameraPreview(true)
            TORCH_MODE -> handleTorch()
            FLASH_MODE -> handleFlash()
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

    // check whether the camera has flash - could be front/back
    fun hasFlash(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() ?: false
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

    private fun handleScaleType() {
        cameraView.scaleType = ResourceUtils.getScaleType(Utils().getInt(SCALE_TYPE, SCALE_TYPE_FIT_CENTER))
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
        }

        ProcessCameraProvider.getInstance(ThisActivity).apply {
            addListener(Runnable {
                val cameraProvider: ProcessCameraProvider = get()

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    cameraSelector = CameraSelector.Builder().requireLensFacing(Utils().getInt(CAMERA_ID, CameraSelector.LENS_FACING_BACK)).build()

                    preview = Preview.Builder()
                            .setTargetAspectRatio(Utils().getInt(ASPECT_RATIO, ASPECT_RATIO_4_3))
                            .build()

                    if (preview != null) {
                        preview!!.setSurfaceProvider(cameraView.createSurfaceProvider())

                        imageCapture = ImageCapture.Builder()
                                .setCaptureMode(Utils().getInt(IMAGE_QUALITY, IMAGE_QUALITY_NORMAL))
                                .setFlashMode(Utils().getInt(FLASH_MODE, FLASH_MODE_AUTO))
                                .setTargetAspectRatio(Utils().getInt(ASPECT_RATIO, ASPECT_RATIO_4_3))
                                .build()

                        if (imageCapture != null) {
                            camera = cameraProvider.bindToLifecycle(ThisActivity as LifecycleOwner, cameraSelector, preview, imageCapture)
                            handleTorch()

                            if (camera != null) {
                                Events.fireCameraReadyEvent(proxy, true)
                            } else {
                                Events.fireCameraReadyEvent(proxy, false, "error_camera_lifecycle")
                            }
                        } else {
                            Events.fireCameraReadyEvent(proxy, false, "error_image_capture")
                        }
                    } else {
                        Events.fireCameraReadyEvent(proxy, false, "error_preview")
                    }

                } catch(exc: Exception) {
                    Log.e(LCAT, "Use case binding failed", exc)
                    Events.fireCameraReadyEvent(proxy, false, null, exc.toString())
                }

            }, MainExecutor)
        }
    }

    fun saveImageAsBitmap(callback: KrollFunction) {
        var messageId = ""
        var isSuccess = false
        var imageBitmap: Bitmap? = null

        imageCapture?.takePicture(MainExecutor, object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeExperimentalUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                if (imageProxy.image == null) {
                    messageId = "error_image_proxy_image"
                } else {
                    imageBitmap = generateBitmap(imageProxy)

                    if (imageBitmap == null) {
                        messageId = "error_bitmap"
                    } else {
                        isSuccess = true
                    }
                }

                callback.call(proxy.krollObject, Methods.CapturePhoto.createResult(imageBitmap, isSuccess, messageId))

                super.onCaptureSuccess(imageProxy)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                val result = Methods.CapturePhoto.createResult(null, false, "error_image_callback")
                callback.callAsync(proxy.krollObject, result)
            }
        })
    }
}
