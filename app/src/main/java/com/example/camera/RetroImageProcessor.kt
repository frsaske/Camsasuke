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
import com.example.model.CameraSettings
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
     * Complete retro photo pipeline:
     * 1. Crop to 4:3 fixed aspect ratio
     * 2. Downscale to target resolution (0.4 MP default, 0.3 MP, or 1.2 MP)
     * 3. Apply Cool Tones retro color matrix curve (100% intensity)
     * 4. Apply 100% digital sensor grain
     * 5. Burn optional vintage amber date stamp on bottom-left
     * 6. Burn permanent "ѕαѕυкє" watermark in bottom-right corner (faded, small, stylish)
     */
    suspend fun processImage(
        source: Bitmap,
        settings: CameraSettings,
        captureTime: Long = System.currentTimeMillis()
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Fixed 4:3 crop
        val cropped = cropTo4x3(source)

        // 2. Downscale to exact chosen MP
        val resized = resizeToResolution(cropped, settings.resolution)

        // 3. Apply Retro Preset (default Cool Tones at 100%)
        val colored = if (settings.preset != RetroPreset.NONE) {
            applyRetroFilter(resized, settings.preset)
        } else {
            resized
        }

        // 4. Apply 100% authentic sensor noise
        val withNoise = applyDigitalNoise(colored)

        // 5. Apply subtle lens vignette
        val withVignette = applySubtleVignette(withNoise)

        // 6. Burn permanent "ѕαѕυкє" watermark and optional date stamp
        val finalImage = applyBadgesAndWatermarks(
            src = withVignette,
            dateStampEnabled = settings.dateStampEnabled,
            captureTime = captureTime
        )

        finalImage
    }

    /**
     * Always 4:3 fixed crop as instructed.
     */
    fun cropTo4x3(src: Bitmap): Bitmap {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val isPortrait = srcH > srcW

        val targetRatio = if (isPortrait) 3f / 4f else 4f / 3f
        val currentRatio = srcW / srcH

        val cropW: Int
        val cropH: Int

        if (currentRatio > targetRatio) {
            cropH = src.height
            cropW = (cropH * targetRatio).roundToInt().coerceAtMost(src.width)
        } else {
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

    fun resizeToResolution(
        src: Bitmap,
        resolution: ResolutionOption
    ): Bitmap {
        val isPortrait = src.height > src.width
        val targetWidth = if (isPortrait) resolution.height4x3 else resolution.width4x3
        val targetHeight = if (isPortrait) resolution.width4x3 else resolution.height4x3

        if (src.width == targetWidth && src.height == targetHeight) {
            return src
        }

        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    fun applyRetroFilter(src: Bitmap, preset: RetroPreset): Bitmap {
        val targetMatrix = when (preset) {
            RetroPreset.COOL_TONES -> ColorMatrix(floatArrayOf(
                0.94f, 0.02f, 0.02f, 0f, 4f,    // Cool early cyber tone
                0.02f, 1.00f, 0.05f, 0f, 6f,
                0.04f, 0.04f, 1.25f, 0f, 18f,   // Lifted CCD blues
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.ORIGINAL_RETRO -> ColorMatrix(floatArrayOf(
                1.08f, 0.02f, 0.00f, 0f, 10f,
                0.02f, 1.06f, 0.02f, 0f, 8f,
                -0.02f, 0.02f, 0.98f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.OLD_DIGITAL_CAMERA -> ColorMatrix(floatArrayOf(
                1.16f, 0.00f, 0.00f, 0f, -4f,
                0.00f, 1.14f, 0.00f, 0f, -4f,
                0.04f, 0.04f, 1.20f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.NOSTALGIC -> ColorMatrix(floatArrayOf(
                1.14f, 0.08f, 0.02f, 0f, 18f,
                0.04f, 1.08f, 0.02f, 0f, 12f,
                -0.06f, 0.02f, 0.90f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.FADED_MEMORY -> ColorMatrix(floatArrayOf(
                0.90f, 0.08f, 0.08f, 0f, 24f,
                0.08f, 0.90f, 0.08f, 0f, 24f,
                0.08f, 0.08f, 0.90f, 0f, 26f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.WARM_TONES -> ColorMatrix(floatArrayOf(
                1.18f, 0.06f, 0.00f, 0f, 16f,
                0.06f, 1.10f, 0.02f, 0f, 10f,
                -0.08f, -0.04f, 0.86f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.NONE -> ColorMatrix()
        }

        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(targetMatrix)
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    fun applyDigitalNoise(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = Random(System.currentTimeMillis())
        val maxDelta = 32 // 100% grain intensity as requested

        for (i in pixels.indices) {
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

    private fun applySubtleVignette(src: Bitmap): Bitmap {
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val radius = sqrt((src.width * src.width + src.height * src.height).toDouble()).toFloat() / 2f
        val maxAlpha = 55

        val shader = RadialGradient(
            src.width / 2f,
            src.height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.argb((maxAlpha * 0.3f).toInt(), 0, 0, 0), Color.argb(maxAlpha, 0, 0, 0)),
            floatArrayOf(0.65f, 0.90f, 1.0f),
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
     * Burns:
     * 1. Optional amber digital date stamp on bottom-left ("23.10.2026")
     * 2. PERMANENT "ѕαѕυкє" watermark in bottom-right corner (faded, small, stylish)
     */
    private fun applyBadgesAndWatermarks(
        src: Bitmap,
        dateStampEnabled: Boolean,
        captureTime: Long
    ): Bitmap {
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val baseTextSize = (src.width * 0.038f).coerceIn(13f, 32f)
        val marginX = (src.width * 0.040f).coerceAtLeast(10f)
        val marginY = (src.height * 0.045f).coerceAtLeast(12f)

        // 1. DATE STAMP (Bottom-Left)
        if (dateStampEnabled) {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
            val dateText = sdf.format(Date(captureTime))

            val datePaint = Paint().apply {
                this.textSize = baseTextSize
                this.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                this.isAntiAlias = true
                this.color = Color.rgb(255, 183, 3) // Vintage Amber
                this.setShadowLayer(baseTextSize * 0.35f, 1.5f, 1.5f, Color.argb(190, 0, 0, 0))
            }

            val textBounds = Rect()
            datePaint.getTextBounds(dateText, 0, dateText.length, textBounds)

            val x = marginX
            val y = src.height.toFloat() - marginY

            val pillPaint = Paint().apply {
                color = Color.argb(90, 0, 0, 0)
                isAntiAlias = true
            }
            val padX = baseTextSize * 0.32f
            val padY = baseTextSize * 0.22f
            val pillRect = RectF(
                x - padX,
                y - textBounds.height() - padY,
                x + textBounds.width() + padX,
                y + padY
            )
            canvas.drawRoundRect(pillRect, 4f, 4f, pillPaint)
            canvas.drawText(dateText, x, y, datePaint)
        }

        // 2. PERMANENT "ѕαѕυкє" WATERMARK (Bottom-Right, Faded & Small)
        val watermarkText = CameraSettings.PERMANENT_WATERMARK // "ѕαѕυкє"
        val watermarkPaint = Paint().apply {
            this.textSize = baseTextSize * 0.95f
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.isAntiAlias = true
            // Faded small text as instructed (alpha ~175)
            this.color = Color.argb(175, 255, 255, 255)
            this.setShadowLayer(baseTextSize * 0.35f, 1.2f, 1.2f, Color.argb(160, 0, 0, 0))
        }

        val bounds = Rect()
        watermarkPaint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)

        val wx = src.width.toFloat() - marginX - bounds.width()
        val wy = src.height.toFloat() - marginY

        // Soft faded dark pill for legibility
        val wPillPaint = Paint().apply {
            color = Color.argb(70, 0, 0, 0)
            isAntiAlias = true
        }
        val wPadX = baseTextSize * 0.30f
        val wPadY = baseTextSize * 0.20f
        val wPillRect = RectF(
            wx - wPadX,
            wy - bounds.height() - wPadY,
            wx + bounds.width() + wPadX,
            wy + wPadY
        )
        canvas.drawRoundRect(wPillRect, 4f, 4f, wPillPaint)
        canvas.drawText(watermarkText, wx, wy, watermarkPaint)

        return output
    }
}
