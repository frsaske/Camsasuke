package com.example

import com.example.model.CameraSettings
import com.example.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTargetResolution0_4MP_matchesApprox400kPixels() {
        val res = ResolutionOption.MP_0_4
        val totalPixels = res.width4x3 * res.height4x3

        // Target: 730 × 548 (approx 400,000 pixels)
        assertEquals(730, res.width4x3)
        assertEquals(548, res.height4x3)
        assertTrue(totalPixels in 395_000..405_000)
    }

    @Test
    fun testStrictlyThreeResolutions() {
        val options = ResolutionOption.values()
        assertEquals(3, options.size)
        assertEquals("0.4 MP", ResolutionOption.MP_0_4.label)
        assertEquals("0.3 MP", ResolutionOption.MP_0_3.label)
        assertEquals("1.2 MP", ResolutionOption.MP_1_2.label)
    }

    @Test
    fun testPermanentWatermarkString() {
        // Must be the exact unicode string requested by user
        assertEquals("ѕαѕυкє", CameraSettings.PERMANENT_WATERMARK)
    }

    @Test
    fun testDefaultSettings() {
        val settings = CameraSettings()
        assertEquals(ResolutionOption.MP_0_4, settings.resolution)
        assertEquals(ResolutionOption.MP_0_4, settings.videoResolution)
        assertTrue(settings.dateStampEnabled)
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
