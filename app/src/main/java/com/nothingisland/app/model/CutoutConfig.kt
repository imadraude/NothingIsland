package com.nothingisland.app.model

import android.content.Context
import android.os.Build
import android.view.WindowManager

/**
 * Calibrated dimensions for Nothing Phone (2a) centered punch-hole camera.
 */
data class CutoutConfig(
    val cameraCenterXOffsetDp: Float = 0f,   // Horizontal offset from screen center (0 = dead center)
    val cameraTopMarginDp: Float = 9f,       // Distance from top of screen to top of camera cutout
    val cameraDiameterDp: Float = 28f,       // Hardware camera cutout diameter (~27-28dp on Nothing 2a)
    val compactPillHeightDp: Float = 34f,    // Sleek height of resting pill (diameter + 6dp OLED bezel)
    val compactMediaWidthDp: Float = 136f,   // Sleek compact width for media playback
    val compactNotifWidthDp: Float = 190f,   // Sleek compact width for notification preview
    val compactBatteryWidthDp: Float = 100f, // Sleek compact width for battery HUD
    val compactTimerWidthDp: Float = 130f,   // Sleek compact width for countdown timer
    val compactVolumeWidthDp: Float = 110f,  // Sleek compact width for volume indicator
    val compactPillWidthDp: Float = 136f,    // Default compact width fallback
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
         * Automatically detects the exact physical punch-hole dimensions from Android WindowMetrics.
         */
        fun detectFromSystem(context: Context): CutoutConfig? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            return try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return null
                val metrics = windowManager.currentWindowMetrics
                val insets = metrics.windowInsets
                val cutout = insets.displayCutout ?: return null
                val rect = cutout.boundingRectTop
                if (rect.isEmpty) return null

                val density = context.resources.displayMetrics.density
                val screenWidthPx = metrics.bounds.width()

                // A punch hole is a circular cutout where width == physical diameter.
                // In Android, rect.top may be 0 if the OEM spans the cutout from screen top edge.
                val diameterPx = rect.width().toFloat()
                val bottomPx = rect.bottom.toFloat()
                val topPx = if (rect.top > 0) rect.top.toFloat() else (bottomPx - diameterPx)
                val centerXPx = rect.centerX().toFloat()
                val screenCenterXPx = screenWidthPx / 2f

                val diameterDp = diameterPx / density
                val topMarginDp = topPx / density
                val centerXOffsetDp = (centerXPx - screenCenterXPx) / density

                // Pill height is camera diameter + 6dp for 3dp top/bottom OLED margins
                val pillHeightDp = (diameterDp + 6f).coerceAtLeast(32f)

                CutoutConfig(
                    cameraCenterXOffsetDp = centerXOffsetDp,
                    cameraTopMarginDp = topMarginDp,
                    cameraDiameterDp = diameterDp,
                    compactPillHeightDp = pillHeightDp,
                    compactMediaWidthDp = 136f,
                    compactNotifWidthDp = 190f,
                    compactBatteryWidthDp = 100f,
                    compactTimerWidthDp = 130f,
                    compactVolumeWidthDp = 110f,
                    compactPillWidthDp = 136f,
                    expandedCardWidthDp = 340f,
                    expandedCardHeightDp = 190f,
                    isAutoDetected = true
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

