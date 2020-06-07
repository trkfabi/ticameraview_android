package ti.cameraview.helper

object Defaults {
    object Properties {
        // properties to be exposed through module
        const val TORCH_MODE = "torchMode"
        const val FLASH_MODE = "flashMode"
    }

    // events and their properties to be exposed through module
    object Events {
        object Image {
            const val NAME = "image"
            const val PROPERTY_IMAGE_PATH = "imagePath"
        }
    }
}