package com.nothingisland.app.model

/**
 * Calibrated dimensions for Nothing Phone (2a) centered punch-hole camera.
 */
data class CutoutConfig(
    val cameraCenterXOffsetDp: Float = 0f,   // Horizontal offset from screen center (0 = dead center)
    val cameraTopMarginDp: Float = 10f,      // Distance from top of screen to top of camera cutout
    val cameraDiameterDp: Float = 34f,       // Hardware camera cutout diameter
    val compactPillHeightDp: Float = 40f,    // Height of resting pill (cam diameter + 6dp OLED bezel)
    val compactMediaWidthDp: Float = 184f,   // Width for media playback (art + visualizer)
    val compactNotifWidthDp: Float = 240f,   // Width for notification preview
    val compactBatteryWidthDp: Float = 144f, // Width for battery HUD
    val compactTimerWidthDp: Float = 168f,   // Width for countdown timer
    val compactVolumeWidthDp: Float = 150f,  // Width for volume indicator
    val compactPillWidthDp: Float = 184f,    // Default compact width fallback
    val expandedCardWidthDp: Float = 356f,   // Expanded card width
    val expandedCardHeightDp: Float = 205f   // Expanded card height ensuring camera clearance
) {
    val cameraCenterYDp: Float
        get() = cameraTopMarginDp + (cameraDiameterDp / 2f)

    val pillTopMarginDp: Float
        get() = (cameraCenterYDp - (compactPillHeightDp / 2f)).coerceAtLeast(0f)
}
