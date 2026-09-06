package com.nothingisland.app.model

/**
 * Calibrated dimensions for Nothing Phone (2a) cutout.
 * Center punch-hole camera at top of display.
 */
data class CutoutConfig(
    val cameraCenterXOffsetDp: Float = 0f, // 0 = perfectly centered horizontally
    val cameraTopMarginDp: Float = 11f,    // Distance from top edge to top of camera cutout
    val cameraDiameterDp: Float = 34f,     // Circular punch-hole diameter
    val compactPillHeightDp: Float = 38f,  // Resting pill height
    val compactPillWidthDp: Float = 160f,  // Resting pill width flanking camera
    val expandedCardWidthDp: Float = 360f, // Expanded card width
    val expandedCardHeightDp: Float = 170f // Expanded card height
)
