package com.example.model

enum class RetroPreset(
    val title: String,
    val description: String
) {
    ORIGINAL_RETRO("Original Retro", "2000s compact camera CMOS look with balanced contrast"),
    OLD_DIGITAL_CAMERA("Old Digital Camera", "Authentic 2004 CCD sensor aesthetic with sharp edge definition"),
    NOSTALGIC("Nostalgic", "Warm golden lift with soft highlights and retro character"),
    FADED_MEMORY("Faded Memory", "Muted nostalgic tones with washed shadows"),
    COOL_TONES("Cool Tones", "Cyber-cool bluish fluorescent white-balance"),
    WARM_TONES("Warm Tones", "Cozy vintage tungsten indoor warmth"),
    NONE("Clean", "Unfiltered original with targeted resolution scaling")
}

enum class AspectRatioOption(val label: String, val ratioFloat: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f)
}

enum class ResolutionOption(
    val label: String,
    val targetPixels: Int,
    val subtitle: String
) {
    MP_0_3("0.3 MP", 300_000, "VGA ~640 × 480"),
    MP_0_4("0.4 MP", 400_000, "Classic ~730 × 548"),
    MP_0_5("0.5 MP", 500_000, "Retro ~800 × 600"),
    MP_0_9("0.9 MP", 900_000, "Nostalgic ~1100 × 825"),
    MP_1_0("1 MP", 1_000_000, "Early Digicam ~1154 × 866"),
    MP_2_0("2 MP", 2_000_000, "2000s Sharp ~1632 × 1224"),
    MP_3_0("3 MP", 3_000_000, "Vintage High-Res ~2000 × 1500")
}

enum class DateStampStyle(val label: String, val hexColor: Long) {
    YELLOW("Retro Amber", 0xFFFFB703),
    ORANGE("Vintage Orange", 0xFFFB8500),
    WHITE("Classic White", 0xFFFFFFFF)
}

enum class DateStampFormat(val label: String, val pattern: String) {
    DD_MM_YYYY("23.10.2026", "dd.MM.yyyy"),
    YY_MM_DD("'26 10 23", "''yy MM dd"),
    YYYY_MM_DD("2026/10/23", "yyyy/MM/dd")
}

enum class CameraFlashMode {
    AUTO,
    ON,
    OFF
}

enum class CameraLensFacing {
    BACK,
    FRONT
}

data class CameraSettings(
    val defaultLens: CameraLensFacing = CameraLensFacing.BACK,
    val resolution: ResolutionOption = ResolutionOption.MP_0_4,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val flashMode: CameraFlashMode = CameraFlashMode.OFF,
    val mirrorSelfiePreview: Boolean = true,
    val saveSelfieMirrored: Boolean = false,
    val preset: RetroPreset = RetroPreset.ORIGINAL_RETRO,
    val filterIntensity: Float = 0.85f,
    val grainAmount: Float = 0.35f,
    val dateStampEnabled: Boolean = true, // Default to vintage date stamp on like in user's photo
    val sasukeWatermarkEnabled: Boolean = true, // Cool Sasuke signature in right corner
    val dateStampStyle: DateStampStyle = DateStampStyle.YELLOW,
    val dateStampFormat: DateStampFormat = DateStampFormat.DD_MM_YYYY,
    val soundEnabled: Boolean = true,
    val recordAudio: Boolean = true,
    val jpegQuality: Int = 92
)
