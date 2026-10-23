package com.example.model

enum class RetroPreset(
    val title: String,
    val description: String
) {
    COOL_TONES("Cool Tones", "Nostalgic early 2000s cyber-cool aesthetic"),
    ORIGINAL_RETRO("Original Retro", "Vintage compact camera CMOS look"),
    OLD_DIGITAL_CAMERA("Old Digital Camera", "2004 CCD sensor sharpness"),
    NOSTALGIC("Nostalgic", "Warm golden lift with soft highlights"),
    FADED_MEMORY("Faded Memory", "Muted nostalgic washed tones"),
    WARM_TONES("Warm Tones", "Cozy vintage tungsten warmth"),
    NONE("Clean", "Unfiltered raw scaling")
}

enum class ResolutionOption(
    val label: String,
    val targetPixels: Int,
    val width4x3: Int,
    val height4x3: Int
) {
    MP_0_4("0.4 MP", 400_000, 730, 548),
    MP_0_3("0.3 MP", 300_000, 640, 480),
    MP_1_2("1.2 MP", 1_200_000, 1264, 948)
}

enum class CameraFlashMode {
    OFF,
    ON,
    AUTO
}

enum class CameraLensFacing {
    BACK,
    FRONT
}

data class CameraSettings(
    val defaultLens: CameraLensFacing = CameraLensFacing.BACK,
    val resolution: ResolutionOption = ResolutionOption.MP_0_4, // Default 0.4 MP
    val videoResolution: ResolutionOption = ResolutionOption.MP_0_4, // Default 0.4 MP for video
    val flashMode: CameraFlashMode = CameraFlashMode.OFF,
    val preset: RetroPreset = RetroPreset.COOL_TONES, // Default Cool Tones
    val filterIntensity: Float = 1.0f, // 100% fixed
    val grainAmount: Float = 1.0f,     // 100% fixed
    val dateStampEnabled: Boolean = true, // Default ON
    val recordAudio: Boolean = true,
    val soundEnabled: Boolean = true,
    val jpegQuality: Int = 92
) {
    companion object {
        // Exact permanent watermark requested by user
        const val PERMANENT_WATERMARK = "ѕαѕυкє"
    }
}
