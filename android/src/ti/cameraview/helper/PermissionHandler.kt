package ti.cameraview.helper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

class PermissionHandler {
    companion object {
        @JvmStatic
        fun hasStoragePermission(): Boolean {
            if (Build.VERSION.SDK_INT >= 23) {
                return ResourceUtils.CONTEXT.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            return true
        }

        @JvmStatic
        fun hasCameraPermission(): Boolean {
            if (Build.VERSION.SDK_INT >= 23) {
                return ResourceUtils.CONTEXT.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
            return true
        }
    }
}