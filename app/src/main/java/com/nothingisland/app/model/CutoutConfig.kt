package com.nothingisland.app.model

import android.content.Context
import com.nothingisland.app.core.cutout.CameraCutoutDetector

/**
 * Calibrated dimensions for Nothing Phone (2a) centered punch-hole camera.
 */
data class CutoutConfig(
    val cameraCenterXOffsetDp: Float = 0f,   // Horizontal offset from screen center (0 = dead center)
    val cameraTopMarginDp: Float = 8.5f,     // Distance from top of screen to top of camera cutout (8.5dp on Nothing 2a)
    val cameraDiameterDp: Float = 22.3f,     // Hardware camera cutout diameter (22.3dp on Nothing 2a)
    val compactPillHeightDp: Float = 26f,    // Sleek, thin 26dp pill height hugging camera punch hole
    val compactMediaWidthDp: Float = 144f,   // Sleek compact width for media playback
    val compactNotifWidthDp: Float = 196f,   // Sleek compact width for notification preview
    val compactBatteryWidthDp: Float = 104f, // Sleek compact width for battery HUD
    val compactTimerWidthDp: Float = 134f,   // Sleek compact width for countdown timer
    val compactVolumeWidthDp: Float = 114f,  // Sleek compact width for volume indicator
    val compactPillWidthDp: Float = 144f,    // Default compact width fallback
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

