package com.nothingisland.app.core.cutout

import com.nothingisland.app.model.CutoutConfig
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
        // Pill height = diameter = 28dp (compact grows horizontally)
        assertEquals(28f, config.compactPillHeightDp, 0.01f)
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
        assertEquals(28f, config.compactPillHeightDp, 0.01f)
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
        assertEquals(28f, config.compactPillHeightDp, 0.01f)
    }

    @Test
    fun chooseDetectionResult_prefersLiveWindowGeometry() {
        val live = CutoutConfig(cameraTopMarginDp = 7f, cameraDiameterDp = 31f)
        val profile = CutoutConfig(cameraTopMarginDp = 9f, cameraDiameterDp = 28f)
        val resource = CutoutConfig(cameraTopMarginDp = 8f, cameraDiameterDp = 30f)

        assertEquals(live, CameraCutoutDetector.chooseDetectionResult(live, profile, resource))
        assertEquals(resource, CameraCutoutDetector.chooseDetectionResult(null, profile, resource))
        assertEquals(profile, CameraCutoutDetector.chooseDetectionResult(null, profile, null))
    }

    @Test
    fun buildConfigFromRawBounds_preservesNonNothingPunchHoleSizes() {
        val small = CameraCutoutDetector.buildConfigFromRawBounds(
            CutoutRawBounds(left = 513f, top = 12f, right = 567f, bottom = 66f),
            displayWidth = 1080,
            density = 3f
        )
        val large = CameraCutoutDetector.buildConfigFromRawBounds(
            CutoutRawBounds(left = 477f, top = 18f, right = 603f, bottom = 144f),
            displayWidth = 1080,
            density = 3f
        )

        assertEquals(18f, small.cameraDiameterDp, 0.01f)
        assertEquals(42f, large.cameraDiameterDp, 0.01f)
    }

    @Test
    fun cutoutConfig_pillTopMargin_ensuresCompleteCameraCoverage() {
        val config = CutoutConfig(
            cameraTopMarginDp = 8f,
            cameraDiameterDp = 28f,
            compactPillHeightDp = 28f
        )
        // camera center = 8 + 14 = 22dp
        // pillTopMargin = 22 - 14 = 8dp
        assertEquals(22f, config.cameraCenterYDp, 0.01f)
        assertEquals(8f, config.pillTopMarginDp, 0.01f)

        // Pill top is at 8dp, camera top is at 8dp -> 0dp bezel above camera
        val topCoverage = config.cameraTopMarginDp - config.pillTopMarginDp
        assertEquals(0f, topCoverage, 0.01f)

        // Pill bottom is at 8 + 28 = 36dp, camera bottom is at 8 + 28 = 36dp -> 0dp bezel below camera
        val pillBottom = config.pillTopMarginDp + config.compactPillHeightDp
        val cameraBottom = config.cameraTopMarginDp + config.cameraDiameterDp
        val bottomCoverage = pillBottom - cameraBottom
        assertEquals(0f, bottomCoverage, 0.01f)
    }

    @Test
    fun buildConfigFromRawBounds_cornerPunchHole_calculatesCorrectOffset() {
        val density = 3.0f
        val displayWidth = 1080 // Center is 540px = 180dp
        // Corner camera at X = 28dp = 84px
        val rawBounds = CutoutRawBounds(left = 42f, top = 30f, right = 126f, bottom = 114f)
        val config = CameraCutoutDetector.buildConfigFromRawBounds(rawBounds, displayWidth, density)
        // Expected offset: 28dp - 180dp = -152dp
        assertEquals(-152f, config.cameraCenterXOffsetDp, 0.01f)
        assertEquals(28f, config.cameraDiameterDp, 0.01f)
        assertEquals(28f, config.compactPillHeightDp, 0.01f)
    }

    @Test
    fun calculateHeuristicBounds_withStatusBarHeight_usesStatusBarHeight() {
        val density = 3.0f
        val displayWidth = 1080
        val bounds = CameraCutoutDetector.calculateHeuristicBounds(
            top = 0f,
            bottom = 120f,
            left = 498f,
            right = 582f,
            displayWidth = displayWidth,
            density = density,
            statusBarHeightPx = 135f // 45dp
        )
        // (45 - 28) / 2 = 8.5dp -> 25.5px
        assertEquals(25.5f, bounds.top, 0.01f)
        assertEquals(84f, bounds.width, 0.01f)
    }

    @Test
    fun buildConfigFromRawBounds_centeredDisplayCutout_remainsZeroOffset() {
        val density = 3.0f
        val displayWidth = 1080
        // Physical cutout is centered on screen: 498px to 582px (center = 540px)
        val rawBounds = CutoutRawBounds(left = 498f, top = 27f, right = 582f, bottom = 111f)
        val config = CameraCutoutDetector.buildConfigFromRawBounds(rawBounds, displayWidth, density)

        // Must remain exactly 0f without any drift
        assertEquals(0f, config.cameraCenterXOffsetDp, 0.0f)
        assertEquals(28f, config.cameraDiameterDp, 0.01f)
    }
}
