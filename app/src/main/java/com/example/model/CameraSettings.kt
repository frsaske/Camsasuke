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
    RETRO_0_4_MP("Retro 0.4 MP", 400_000, "~730 × 548 vintage digital resolution"),
    LOW_1_2_MP("Low 1.2 MP", 1_200_000, "Early camera phone / web resolution"),
    STANDARD("Standard", -1, "Native device camera output")
}

enum class DateStampStyle(val label: String, val hexColor: Long) {
    YELLOW("Retro Yellow", 0xFFFFB703),
    ORANGE("Vintage Orange", 0xFFFB8500),
    WHITE("Classic White", 0xFFFFFFFF)
}

enum class DateStampFormat(val label: String, val pattern: String) {
    DD_MM_YYYY("22.10.2026", "dd.MM.yyyy"),
    YY_MM_DD("'26 10 22", "''yy MM dd"),
    YYYY_MM_DD("2026/10/22", "yyyy/MM/dd"),
    DD_MM_YY_TIME("22.10.'26 14:30", "dd.MM.''yy HH:mm")
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
    val resolution: ResolutionOption = ResolutionOption.RETRO_0_4_MP,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val flashMode: CameraFlashMode = CameraFlashMode.AUTO,
    val mirrorSelfiePreview: Boolean = true,
    val saveSelfieMirrored: Boolean = false,
    val showGrid: Boolean = false,
    val showVintageHud: Boolean = true,
    val preset: RetroPreset = RetroPreset.ORIGINAL_RETRO,
    val filterIntensity: Float = 0.85f,
    val grainAmount: Float = 0.30f,
    val vignetteAmount: Float = 0.25f,
    val dateStampEnabled: Boolean = false, // Disabled by default per requirements
    val dateStampStyle: DateStampStyle = DateStampStyle.YELLOW,
    val dateStampFormat: DateStampFormat = DateStampFormat.DD_MM_YYYY,
    val jpegQuality: Int = 90,
    val recordAudio: Boolean = true,
    val videoQuality: String = "HD 720p"
)
