package ti.cameraview.helper

object Defaults {
    // properties to be exposed through module
    const val PROPERTY_TORCH_MODE = "torchMode"
    const val PROPERTY_FLASH_MODE = "flashMode"

    // events and their properties to be exposed through module
    object Events {
        object Image {
            const val EVENT_NAME = "image"
            const val EVENT_PROPERTY_IMAGE_PATH = "imagePath"
        }
    }
}