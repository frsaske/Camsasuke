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
     * Complete pipeline:
     * 1. Mirror if selfie
     * 2. Crop to aspect ratio (4:3, 16:9, 1:1)
     * 3. Downscale to exact target resolution (0.3 MP, 0.4 MP, 0.5 MP, 0.9 MP, 1 MP, 2 MP, 3 MP)
     * 4. Apply consistent vintage CCD retro tone curve
     * 5. Apply authentic sensor grain
     * 6. Burn optional date stamp on bottom-left corner
     * 7. Burn cool stylish Sasuke signature on bottom-right corner
     */
    suspend fun processImage(
        source: Bitmap,
        settings: CameraSettings,
        isFrontCamera: Boolean = false,
        captureTime: Long = System.currentTimeMillis()
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Mirroring if requested
        val oriented = if (isFrontCamera && settings.saveSelfieMirrored) {
            val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        // 2. Crop to aspect ratio
        val cropped = cropToAspectRatio(oriented, settings.aspectRatio)

        // 3. Resize to exact chosen MP resolution (e.g. 0.4 MP = ~730 x 548)
        val resized = resizeToTargetResolution(cropped, settings.resolution, settings.aspectRatio)

        // 4. Apply retro tone curve
        val colored = if (settings.preset != RetroPreset.NONE && settings.filterIntensity > 0.05f) {
            applyRetroFilter(resized, settings.preset, settings.filterIntensity)
        } else {
            resized
        }

        // 5. Apply consistent authentic sensor noise
        val withNoise = if (settings.grainAmount > 0.05f) {
            applyDigitalNoise(colored, settings.grainAmount)
        } else {
            colored
        }

        // 6. Optional Vignette
        val withVignette = applySubtleVignette(withNoise)

        // 7. Overlays: Date stamp (bottom-left) and Cool Sasuke text (bottom-right)
        val finalImage = applyBadgesAndWatermarks(
            src = withVignette,
            settings = settings,
            captureTime = captureTime
        )

        finalImage
    }

    fun cropToAspectRatio(src: Bitmap, aspect: AspectRatioOption): Bitmap {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val isPortrait = srcH > srcW

        val targetRatio = if (isPortrait) 1f / aspect.ratioFloat else aspect.ratioFloat
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

    fun resizeToTargetResolution(
        src: Bitmap,
        resolution: ResolutionOption,
        aspect: AspectRatioOption
    ): Bitmap {
        val targetPixels = resolution.targetPixels.toDouble()
        val isPortrait = src.height > src.width
        val ratio = if (isPortrait) 1.0 / aspect.ratioFloat else aspect.ratioFloat.toDouble()

        val targetHeight = sqrt(targetPixels / ratio).roundToInt()
        val targetWidth = (targetHeight * ratio).roundToInt()

        if (src.width == targetWidth && src.height == targetHeight) {
            return src
        }

        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    fun applyRetroFilter(src: Bitmap, preset: RetroPreset, intensity: Float): Bitmap {
        // High-fidelity early 2000s compact camera CCD curve
        val targetMatrix = when (preset) {
            RetroPreset.ORIGINAL_RETRO -> ColorMatrix(floatArrayOf(
                1.08f, 0.02f, 0.00f, 0f, 10f,   // Warm CCD dynamic pop
                0.02f, 1.06f, 0.02f, 0f, 8f,
                -0.02f, 0.02f, 0.98f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.OLD_DIGITAL_CAMERA -> ColorMatrix(floatArrayOf(
                1.16f, 0.00f, 0.00f, 0f, -4f,   // Punchy high-contrast digital cam
                0.00f, 1.14f, 0.00f, 0f, -4f,
                0.04f, 0.04f, 1.20f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.NOSTALGIC -> ColorMatrix(floatArrayOf(
                1.14f, 0.08f, 0.02f, 0f, 18f,  // Warm golden lift
                0.04f, 1.08f, 0.02f, 0f, 12f,
                -0.06f, 0.02f, 0.90f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.FADED_MEMORY -> ColorMatrix(floatArrayOf(
                0.90f, 0.08f, 0.08f, 0f, 24f,  // Washed faded shadows
                0.08f, 0.90f, 0.08f, 0f, 24f,
                0.08f, 0.08f, 0.90f, 0f, 26f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.COOL_TONES -> ColorMatrix(floatArrayOf(
                0.92f, 0.02f, 0.04f, 0f, -2f,
                0.02f, 0.98f, 0.06f, 0f, 4f,
                0.04f, 0.04f, 1.22f, 0f, 16f,  // Cool early cyber tone
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.WARM_TONES -> ColorMatrix(floatArrayOf(
                1.18f, 0.06f, 0.00f, 0f, 16f,  // Tungsten vintage warmth
                0.06f, 1.10f, 0.02f, 0f, 10f,
                -0.08f, -0.04f, 0.86f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
            ))
            RetroPreset.NONE -> ColorMatrix()
        }

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

    fun applyDigitalNoise(src: Bitmap, amount: Float): Bitmap {
        val width = src.width
        val height = src.height
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = Random(System.currentTimeMillis())
        val maxDelta = (28 * amount.coerceIn(0f, 1f)).toInt()
        if (maxDelta <= 0) return src

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
        val maxAlpha = 65

        val shader = RadialGradient(
            src.width / 2f,
            src.height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.argb((maxAlpha * 0.3f).toInt(), 0, 0, 0), Color.argb(maxAlpha, 0, 0, 0)),
            floatArrayOf(0.60f, 0.88f, 1.0f),
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
     * 1. Date stamp on the bottom-left (e.g. "23.10.2026")
     * 2. Cool stylish "⚡ Sasuke" signature on the bottom-right
     */
    private fun applyBadgesAndWatermarks(
        src: Bitmap,
        settings: CameraSettings,
        captureTime: Long
    ): Bitmap {
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // Base text size scaled cleanly to image width
        val baseTextSize = (src.width * 0.038f).coerceIn(13f, 32f)
        val marginX = (src.width * 0.040f).coerceAtLeast(10f)
        val marginY = (src.height * 0.045f).coerceAtLeast(12f)

        // 1. DATE STAMP (Bottom-Left)
        if (settings.dateStampEnabled) {
            val sdf = SimpleDateFormat(settings.dateStampFormat.pattern, Locale.US)
            val dateText = sdf.format(Date(captureTime))

            val datePaint = Paint().apply {
                this.textSize = baseTextSize
                this.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                this.isAntiAlias = true
                this.color = settings.dateStampStyle.hexColor.toInt()
                this.setShadowLayer(baseTextSize * 0.35f, 1.5f, 1.5f, Color.argb(180, 0, 0, 0))
            }

            val textBounds = Rect()
            datePaint.getTextBounds(dateText, 0, dateText.length, textBounds)

            val x = marginX
            val y = src.height.toFloat() - marginY

            // Translucent dark back-pill
            val pillPaint = Paint().apply {
                color = Color.argb(95, 0, 0, 0)
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

        // 2. COOL SASUKE SIGNATURE (Bottom-Right)
        if (settings.sasukeWatermarkEnabled) {
            val sasukeText = "⚡ Sasuke"
            val sasukePaint = Paint().apply {
                this.textSize = baseTextSize * 0.95f
                this.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
                this.isAntiAlias = true
                this.color = Color.argb(230, 255, 255, 255)
                this.setShadowLayer(baseTextSize * 0.4f, 1.5f, 1.5f, Color.argb(200, 0, 0, 0))
            }

            val bounds = Rect()
            sasukePaint.getTextBounds(sasukeText, 0, sasukeText.length, bounds)

            val x = src.width.toFloat() - marginX - bounds.width()
            val y = src.height.toFloat() - marginY

            // Dark translucent back-pill
            val pillPaint = Paint().apply {
                color = Color.argb(110, 10, 10, 15)
                isAntiAlias = true
            }
            val padX = baseTextSize * 0.35f
            val padY = baseTextSize * 0.22f
            val pillRect = RectF(
                x - padX,
                y - bounds.height() - padY,
                x + bounds.width() + padX,
                y + padY
            )
            canvas.drawRoundRect(pillRect, 4f, 4f, pillPaint)
            canvas.drawText(sasukeText, x, y, sasukePaint)
        }

        return output
    }
}
