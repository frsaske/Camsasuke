package com.example.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.AspectRatioOption
import com.example.model.CameraSettings
import com.example.model.DateStampFormat
import com.example.model.DateStampStyle
import com.example.model.ResolutionOption
import com.example.model.RetroPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.roundToInt
import kotlin.math.sqrt

object RetroImageProcessor {

    /**
     * Complete pipeline: crop aspect ratio -> resize to target resolution (e.g. 0.4 MP)
     * -> apply retro color preset with intensity -> apply subtle digital sensor noise
     * -> apply subtle vintage vignette -> optional date stamp.
     */
    suspend fun processImage(
        source: Bitmap,
        settings: CameraSettings,
        isFrontCamera: Boolean = false,
        captureTime: Long = System.currentTimeMillis()
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Mirroring if front camera and requested
        val oriented = if (isFrontCamera && settings.saveSelfieMirrored) {
            val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        // 2. Crop to aspect ratio
        val cropped = cropToAspectRatio(oriented, settings.aspectRatio)

        // 3. Resize to selected resolution (AUTHENTIC 0.4 MP or 1.2 MP or Standard)
        val resized = resizeToTargetResolution(cropped, settings.resolution, settings.aspectRatio)

        // 4. Apply retro preset and color matrix
        val colored = if (settings.preset != RetroPreset.NONE && settings.filterIntensity > 0.05f) {
            applyRetroFilter(resized, settings.preset, settings.filterIntensity)
        } else {
            resized
        }

        // 5. Apply digital noise / sensor grain
        val withNoise = if (settings.grainAmount > 0.05f) {
            applyDigitalNoise(colored, settings.grainAmount)
        } else {
            colored
        }

        // 6. Apply subtle vintage vignette
        val withVignette = if (settings.vignetteAmount > 0.05f) {
            applyVignette(withNoise, settings.vignetteAmount)
        } else {
            withNoise
        }

        // 7. Optional date stamp (burned into image ONLY when enabled)
        val finalImage = if (settings.dateStampEnabled) {
            applyDateStamp(withVignette, settings.dateStampStyle, settings.dateStampFormat, captureTime)
        } else {
            withVignette
        }

        finalImage
    }

    /**
     * Crops the center of the bitmap to match the requested aspect ratio.
     */
    fun cropToAspectRatio(src: Bitmap, aspect: AspectRatioOption): Bitmap {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val isPortrait = srcH > srcW

        val targetRatio = if (isPortrait) {
            1f / aspect.ratioFloat
        } else {
            aspect.ratioFloat
        }

        val currentRatio = srcW / srcH

        val cropW: Int
        val cropH: Int

        if (currentRatio > targetRatio) {
            // Wider than target ratio: crop width
            cropH = src.height
            cropW = (cropH * targetRatio).roundToInt().coerceAtMost(src.width)
        } else {
            // Taller than target ratio: crop height
            cropW = src.width
            cropH = (cropW / targetRatio).roundToInt().coerceAtMost(src.height)
        }

        val x = ((src.width - cropW) / 2).coerceAtLeast(0)
        val y = ((src.height - cropH) / 2).coerceAtLeast(0)

        if (cropW == src.width && cropH == src.height) {
            return src
        }

        return Bitmap.createBitmap(src, x, y, cropW, cropH)
    }

    /**
     * Resizes the image to the exact targeted resolution.
     * For Retro 0.4 MP (~400,000 pixels):
     * 4:3 Landscape -> 730 × 548 (400,040 pixels)
     * 4:3 Portrait  -> 548 × 730 (400,040 pixels)
     * 16:9 Landscape -> 842 × 474 (399,108 pixels)
     * 1:1 Square     -> 632 × 632 (399,424 pixels)
     */
    fun resizeToTargetResolution(
        src: Bitmap,
        resolution: ResolutionOption,
        aspect: AspectRatioOption
    ): Bitmap {
        if (resolution == ResolutionOption.STANDARD) {
            return src
        }

        val targetPixels = resolution.targetPixels.toDouble()
        val isPortrait = src.height > src.width
        val ratio = if (isPortrait) 1.0 / aspect.ratioFloat else aspect.ratioFloat.toDouble()

        // width * height = targetPixels, and width / height = ratio
        // height = sqrt(targetPixels / ratio), width = height * ratio
        val targetHeight = sqrt(targetPixels / ratio).roundToInt()
        val targetWidth = (targetHeight * ratio).roundToInt()

        // Avoid upscaling if source is already smaller
        if (src.width <= targetWidth && src.height <= targetHeight) {
            return src
        }

        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    /**
     * Applies retro color matrix with intensity interpolation.
     */
    fun applyRetroFilter(src: Bitmap, preset: RetroPreset, intensity: Float): Bitmap {
        val targetMatrix = when (preset) {
            RetroPreset.ORIGINAL_RETRO -> ColorMatrix(floatArrayOf(
                1.05f, 0.05f, -0.02f, 0f, 8f,    // Slight boost in reds and warm baseline
                0.02f, 1.02f, 0.02f, 0f, 6f,     // Clean green
                -0.03f, 0.02f, 0.95f, 0f, -4f,   // Slightly attenuated blue (vintage warm tone)
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.OLD_DIGITAL_CAMERA -> ColorMatrix(floatArrayOf(
                1.15f, 0.00f, 0.00f, 0f, -8f,    // Crunchy CCD contrast
                0.00f, 1.12f, 0.00f, 0f, -8f,    // High dynamic curve
                0.05f, 0.05f, 1.18f, 0f, -2f,    // Punchy cool-blue CCD highlights
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.NOSTALGIC -> ColorMatrix(floatArrayOf(
                1.12f, 0.08f, 0.02f, 0f, 15f,   // Golden warm lift
                0.04f, 1.05f, 0.02f, 0f, 10f,
                -0.08f, 0.02f, 0.88f, 0f, 2f,   // Amber glow
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.FADED_MEMORY -> ColorMatrix(floatArrayOf(
                0.88f, 0.10f, 0.08f, 0f, 22f,   // Lifted washed shadows
                0.08f, 0.88f, 0.08f, 0f, 22f,
                0.08f, 0.10f, 0.88f, 0f, 26f,   // Low contrast washed look
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.COOL_TONES -> ColorMatrix(floatArrayOf(
                0.90f, 0.02f, 0.05f, 0f, -4f,   // Dimmed reds
                0.02f, 0.98f, 0.08f, 0f, 4f,
                0.05f, 0.05f, 1.25f, 0f, 18f,   // Cyber early 2000s cool fluorescent tint
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.WARM_TONES -> ColorMatrix(floatArrayOf(
                1.20f, 0.06f, 0.00f, 0f, 14f,   // Cozy tungsten amber
                0.08f, 1.10f, 0.02f, 0f, 8f,
                -0.10f, -0.05f, 0.82f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.NONE -> ColorMatrix()
        }

        // Interpolate between Identity matrix and Target matrix based on intensity
        val identity = ColorMatrix()
        val finalMatrix = ColorMatrix()
        val iArr = identity.array
        val tArr = targetMatrix.array
        val resArr = FloatArray(20)

        for (i in 0 until 20) {
            resArr[i] = iArr[i] + (tArr[i] - iArr[i]) * intensity.coerceIn(0f, 1f)
        }
        finalMatrix.set(resArr)

        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(finalMatrix)
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Applies subtle digital sensor noise characteristic of early digital compact cameras.
     */
    fun applyDigitalNoise(src: Bitmap, amount: Float): Bitmap {
        val width = src.width
        val height = src.height
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = Random(42)
        val maxDelta = (32 * amount.coerceIn(0f, 1f)).toInt()
        if (maxDelta <= 0) return src

        for (i in pixels.indices) {
            // Apply slight sensor noise (luminance variation)
            val noise = random.nextInt(maxDelta * 2 + 1) - maxDelta
            val p = pixels[i]
            val a = (p shr 24) and 0xFF
            var r = ((p shr 16) and 0xFF) + noise
            var g = ((p shr 8) and 0xFF) + noise
            var b = (p and 0xFF) + noise

            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Applies subtle lens vignette (corner falloff).
     */
    fun applyVignette(src: Bitmap, amount: Float): Bitmap {
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val radius = sqrt((src.width * src.width + src.height * src.height).toDouble()).toFloat() / 2f
        val maxAlpha = (120 * amount.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)

        val shader = RadialGradient(
            src.width / 2f,
            src.height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.argb((maxAlpha * 0.4f).toInt(), 0, 0, 0), Color.argb(maxAlpha, 0, 0, 0)),
            floatArrayOf(0.55f, 0.85f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            this.shader = shader
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), paint)
        return output
    }

    /**
     * Burns an authentic old digital camera date stamp in the bottom-left corner.
     * E.g. "22.10.2026" or "'26 10 22" in retro glowing digital style.
     */
    fun applyDateStamp(
        src: Bitmap,
        style: DateStampStyle,
        format: DateStampFormat,
        timestamp: Long
    ): Bitmap {
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val sdf = SimpleDateFormat(format.pattern, Locale.US)
        val dateText = sdf.format(Date(timestamp))

        // Scale font size based on image width (e.g., ~3.5% of width, minimum 14px)
        val textSize = (src.width * 0.038f).coerceAtLeast(14f)
        val marginX = (src.width * 0.045f).coerceAtLeast(12f)
        val marginY = (src.height * 0.055f).coerceAtLeast(16f)

        // Monospace retro digital look
        val typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        val textPaint = Paint().apply {
            this.textSize = textSize
            this.typeface = typeface
            this.isAntiAlias = true
            this.color = style.hexColor.toInt()
            // Subtle glowing shadow typical of digital LCD phosphor
            this.setShadowLayer(textSize * 0.25f, 1f, 1f, Color.argb(160, 0, 0, 0))
        }

        val textBounds = Rect()
        textPaint.getTextBounds(dateText, 0, dateText.length, textBounds)

        val x = marginX
        val y = src.height.toFloat() - marginY

        // Draw subtle soft dark background pill for crisp legibility
        val bgPaint = Paint().apply {
            color = Color.argb(90, 0, 0, 0)
            isAntiAlias = true
        }
        val pillPaddingX = textSize * 0.35f
        val pillPaddingY = textSize * 0.25f
        val pillRect = RectF(
            x - pillPaddingX,
            y - textBounds.height() - pillPaddingY,
            x + textBounds.width() + pillPaddingX,
            y + pillPaddingY
        )
        canvas.drawRoundRect(pillRect, 4f, 4f, bgPaint)

        // Draw date text
        canvas.drawText(dateText, x, y, textPaint)

        return output
    }
}
