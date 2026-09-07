package com.nothingisland.app.core.cutout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCutoutDetectorTest {

    @Test
    fun calculateHeuristicBounds_whenTopIsZero_centersCameraInStatusBar() {
        val density = 3.0f
        val displayWidth = 1080
        // Status bar height 45dp = 135px, camera width 28dp = 84px
        val left = (1080f / 2f) - (84f / 2f) // 498px
        val right = (1080f / 2f) + (84f / 2f) // 582px
        val top = 0f
        val bottom = 135f // 45dp

        val bounds = CameraCutoutDetector.calculateHeuristicBounds(
            top = top,
            bottom = bottom,
            left = left,
            right = right,
            displayWidth = displayWidth,
            density = density
        )

        // Estimated top margin: (45 - 28) / 2 = 8.5dp -> 25.5px
        val topMarginDp = bounds.top / density
        assertEquals(8.5f, topMarginDp, 0.01f)
        assertEquals(28.0f, bounds.width / density, 0.01f)
        assertEquals(540f, bounds.centerX, 0.01f)
    }

    @Test
    fun calculateHeuristicBounds_whenTopIsGreaterThanZero_respectsHardwareTop() {
        val density = 2.625f // ~420 dpi
        val displayWidth = 1080
        val top = 24f
        val bottom = 96f
        val left = 504f
        val right = 576f

        val bounds = CameraCutoutDetector.calculateHeuristicBounds(
            top = top,
            bottom = bottom,
            left = left,
            right = right,
            displayWidth = displayWidth,
            density = density
        )

        assertEquals(24f, bounds.top, 0.01f)
        assertEquals(72f, bounds.width, 0.01f)
        assertEquals(540f, bounds.centerX, 0.01f)
    }

    @Test
    fun buildConfigFromRawBounds_producesAccurateDimensions() {
        val density = 3.0f
        val displayWidth = 1080
        val rawBounds = CutoutRawBounds(
            left = 498f,
            top = 27f,
            right = 582f,
            bottom = 111f
        )

        val config = CameraCutoutDetector.buildConfigFromRawBounds(
            bounds = rawBounds,
            displayWidth = displayWidth,
            density = density
        )

        assertEquals(0f, config.cameraCenterXOffsetDp, 0.01f)
        assertEquals(9f, config.cameraTopMarginDp, 0.01f)
        assertEquals(28f, config.cameraDiameterDp, 0.01f)
        // Pill height = diameter + 6dp = 34dp
        assertEquals(34f, config.compactPillHeightDp, 0.01f)
        assertTrue(config.isAutoDetected)
    }

    @Test
    fun buildConfigFromRawBounds_withHorizontalOffset_calculatesCorrectOffset() {
        val density = 3.0f
        val displayWidth = 1080
        // Camera shifted to the left by 30dp (90px)
        val centerXPx = (1080f / 2f) - 90f // 450px
        val diameterPx = 84f // 28dp
        val rawBounds = CutoutRawBounds(
            left = centerXPx - 42f,
            top = 27f,
            right = centerXPx + 42f,
            bottom = 111f
        )

        val config = CameraCutoutDetector.buildConfigFromRawBounds(
            bounds = rawBounds,
            displayWidth = displayWidth,
            density = density
        )

        assertEquals(-30f, config.cameraCenterXOffsetDp, 0.01f)
        assertEquals(9f, config.cameraTopMarginDp, 0.01f)
        assertEquals(28f, config.cameraDiameterDp, 0.01f)
    }

    @Test
    fun buildConfigFromRawBounds_nearCenter_snapsToZeroOffset() {
        val density = 3.0f
        val displayWidth = 1080
        // Cutout slightly offset by 2dp (6px) due to subpixel rendering or OEM rounding
        val centerXPx = (1080f / 2f) + 6f // 546px
        val diameterPx = 84f // 28dp
        val rawBounds = CutoutRawBounds(
            left = centerXPx - 42f,
            top = 27f,
            right = centerXPx + 42f,
            bottom = 111f
        )

        val config = CameraCutoutDetector.buildConfigFromRawBounds(
            bounds = rawBounds,
            displayWidth = displayWidth,
            density = density
        )

        // Must snap strictly to 0f to eliminate jitter and misalignment
        assertEquals(0f, config.cameraCenterXOffsetDp, 0.0f)
    }
}
