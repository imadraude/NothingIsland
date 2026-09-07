package com.nothingisland.app.model

import android.content.Context
import com.nothingisland.app.core.cutout.CameraCutoutDetector

/**
 * Calibrated dimensions for Nothing Phone (2a) centered punch-hole camera.
 */
data class CutoutConfig(
    val cameraCenterXOffsetDp: Float = 0f,   // Horizontal offset from screen center (0 = dead center)
    val cameraTopMarginDp: Float = 12.57f,   // Distance from top of screen to top of camera cutout (12.57dp on Nothing 2a)
    val cameraDiameterDp: Float = 22.1f,     // Hardware camera cutout diameter (22.1dp on Nothing 2a)
    val compactPillHeightDp: Float = 38f,    // Perfectly centered 38dp pill height (dynamicSpot standard with 8dp padding above & below)
    val compactMediaWidthDp: Float = 160f,   // Compact width for media playback
    val compactNotifWidthDp: Float = 200f,   // Compact width for notification preview
    val compactBatteryWidthDp: Float = 120f, // Compact width for battery HUD
    val compactTimerWidthDp: Float = 136f,   // Compact width for countdown timer
    val compactVolumeWidthDp: Float = 120f,  // Compact width for volume indicator
    val compactPillWidthDp: Float = 160f,    // Default compact width fallback
    val expandedCardWidthDp: Float = 340f,   // Expanded card width
    val expandedCardHeightDp: Float = 190f,  // Expanded card height ensuring camera clearance
    val isAutoDetected: Boolean = false      // True if auto-detected from hardware
) {
    val cameraCenterYDp: Float
        get() = cameraTopMarginDp + (cameraDiameterDp / 2f)

    val pillTopMarginDp: Float
        get() = (cameraCenterYDp - (compactPillHeightDp / 2f)).coerceAtLeast(0f)

    companion object {
        /**
         * Automatically detects the exact physical punch-hole dimensions from system Window / Insets / AOSP config.
         */
        fun detectFromSystem(context: Context): CutoutConfig? {
            return CameraCutoutDetector.detectFromContext(context)
        }
    }
}

