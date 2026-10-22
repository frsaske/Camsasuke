package com.example

import com.example.model.AspectRatioOption
import com.example.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ExampleUnitTest {
    @Test
    fun testRetroResolution4_3_matchesApprox400kPixels() {
        val targetPixels = ResolutionOption.RETRO_0_4_MP.targetPixels.toDouble()
        val ratio = AspectRatioOption.RATIO_4_3.ratioFloat.toDouble()
        val targetHeight = sqrt(targetPixels / ratio).roundToInt()
        val targetWidth = (targetHeight * ratio).roundToInt()
        val totalPixels = targetWidth * targetHeight

        // Target: approximately 730 × 548 (approx 400,000 pixels)
        assertTrue(targetWidth in 728..732)
        assertTrue(targetHeight in 546..550)
        assertTrue(totalPixels in 395_000..405_000)
    }

    @Test
    fun testRetroResolution16_9_matchesApprox400kPixels() {
        val targetPixels = ResolutionOption.RETRO_0_4_MP.targetPixels.toDouble()
        val ratio = AspectRatioOption.RATIO_16_9.ratioFloat.toDouble()
        val targetHeight = sqrt(targetPixels / ratio).roundToInt()
        val targetWidth = (targetHeight * ratio).roundToInt()
        val totalPixels = targetWidth * targetHeight

        // Should be approximately 400,000 total pixels
        assertTrue(totalPixels in 390_000..410_000)
    }

    @Test
    fun testRetroResolution1_1_matchesApprox400kPixels() {
        val targetPixels = ResolutionOption.RETRO_0_4_MP.targetPixels.toDouble()
        val ratio = AspectRatioOption.RATIO_1_1.ratioFloat.toDouble()
        val targetHeight = sqrt(targetPixels / ratio).roundToInt()
        val targetWidth = (targetHeight * ratio).roundToInt()
        val totalPixels = targetWidth * targetHeight

        assertEquals(targetWidth, targetHeight)
        assertTrue(totalPixels in 390_000..410_000)
    }

    @Test
    fun testSequentialFilenameFormat() {
        val photoIndex = 1
        val photoName = String.format("Sasuke-%03d.jpg", photoIndex)
        assertEquals("Sasuke-001.jpg", photoName)

        val videoIndex = 42
        val videoName = String.format("Sasuke-Video-%03d.mp4", videoIndex)
        assertEquals("Sasuke-Video-042.mp4", videoName)
    }
}
