package ti.cameraview.camera

import android.util.Log
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Rational
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.view.doOnLayout
import androidx.core.content.ContextCompat
import org.appcelerator.kroll.KrollDict
import org.appcelerator.kroll.KrollProxy
import org.appcelerator.titanium.TiBaseActivity
import org.appcelerator.titanium.TiC
import org.appcelerator.titanium.proxy.TiViewProxy
import org.appcelerator.titanium.util.TiActivityResultHandler
import org.appcelerator.titanium.util.TiConvert
import org.appcelerator.titanium.view.TiCompositeLayout
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement
import org.appcelerator.titanium.view.TiUIView
import ti.cameraview.helper.Defaults
import ti.cameraview.helper.PermissionHandler
import ti.cameraview.helper.ResourceUtils
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.TorchState.OFF
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.TextureViewMeteringPointFactory
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import org.appcelerator.titanium.TiApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class CameraView(proxy: TiViewProxy) : TiUIView(proxy) {
    private var rootView: TiCompositeLayout
    private lateinit var cameraView: PreviewView
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    // getter
    private val isCameraViewAvailable get() = this::cameraView.isInitialized

    companion object {
        val LCAT = "CameraView"
        val REQUEST_CODE_PERMISSIONS = 100
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()

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
        rootView = TiCompositeLayout(proxy.activity, arrangement)

        if (PermissionHandler.hasCameraPermission() && PermissionHandler.hasStoragePermission()) {
            Log.d(LCAT, "****** creating camera-view 1…")
            createCameraPreview()
        }

        setNativeView(rootView)
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

    override fun propertyChanged(key: String, oldValue: Any?, newValue: Any?, proxy: KrollProxy) {
        if (outerView == null) {
            return
        }

        if (key == "color") {
            Log.d(LCAT, "propertyChanged: color = $newValue")
            setColor(TiConvert.toColor(newValue?.toString()))
        }
    }

    private fun setColor(color: Int) {
        outerView.setBackgroundColor(color)
    }

    fun createCameraPreview() {
        // avoid re-creating the camera-view
        if (isCameraViewAvailable) {
            Log.d(LCAT, "****** camera-view already added…")
            return
        }

        Log.d(LCAT, "****** creating camera-view 2 …")

        val layoutParams = TiCompositeLayout.LayoutParams()
        layoutParams.autoFillsHeight = true
        layoutParams.autoFillsWidth = true
        cameraView = PreviewView(proxy.activity)
        rootView.addView(cameraView, layoutParams)
        Log.d(LCAT, "****** camera-view added…")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(TiApplication.getAppCurrentActivity())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

                val aspect = AspectRatio.RATIO_16_9

                preview = Preview.Builder()
                        .setTargetAspectRatio(aspect)
                        .build()

                preview?.setSurfaceProvider(cameraView.createSurfaceProvider())

                imageCapture = ImageCapture.Builder()
                        .setFlashMode(ImageCapture.FLASH_MODE_ON)
                        .setTargetAspectRatio(aspect)
                        .build()

                // sets the image output dimensions ratio { DOES NOT WORK PROPERLY IN ASPECT RATIO 16_9 }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageCapture?.setCropAspectRatio(Rational(1, 1))
                }

                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON

                camera = cameraProvider.bindToLifecycle(TiApplication.getAppCurrentActivity() as LifecycleOwner, cameraSelector, preview, imageCapture)
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON

                // autoFocusOnInterval()
                cameraView.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        return@setOnTouchListener false
                    }

                    if (event.action == MotionEvent.ACTION_DOWN) {
                        autoFocusOnTap(v, event)
                        true
                    }

                    false
                }

            } catch(exc: Exception) {
                Log.e(LCAT, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(proxy.activity))

        Log.d(LCAT, "****** startCamera…")
    }

    @SuppressLint("RestrictedApi")
    private fun autoFocusOnTap(view: View?, event: MotionEvent) {
        cameraView.doOnLayout {
            Log.i(LCAT, "view finder : ${cameraView.width} : ${cameraView.height}")
        }

        val meteringFactory = SurfaceOrientedMeteringPointFactory(cameraView.width.toFloat(), cameraView.height.toFloat())
        val meteringPoint = meteringFactory.createPoint(event.x, event.y);
        val size = MeteringPointFactory.getDefaultPointSize()

        Log.i(LCAT, "**** Meter Data: ${size} : ${meteringPoint.x} : ${meteringPoint.y} :: Event ${event.x}: ${event.y}")

        val action = FocusMeteringAction.Builder(meteringPoint)
                .disableAutoCancel()    // disable auto-focus onwards when this metering action is executed
//            .setAutoCancelDuration(5, TimeUnit.SECONDS)   // resume auto-focus
                .build()

        camera?.cameraControl?.startFocusAndMetering(action)
    }
}
