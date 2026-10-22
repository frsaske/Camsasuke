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
        val targetPixels = ResolutionOption.MP_0_4.targetPixels.toDouble()
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
    fun testMpOptionsExist() {
        assertEquals(7, ResolutionOption.values().size)
        assertEquals("0.3 MP", ResolutionOption.MP_0_3.label)
        assertEquals("0.4 MP", ResolutionOption.MP_0_4.label)
        assertEquals("0.5 MP", ResolutionOption.MP_0_5.label)
        assertEquals("0.9 MP", ResolutionOption.MP_0_9.label)
        assertEquals("1 MP", ResolutionOption.MP_1_0.label)
        assertEquals("2 MP", ResolutionOption.MP_2_0.label)
        assertEquals("3 MP", ResolutionOption.MP_3_0.label)
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
