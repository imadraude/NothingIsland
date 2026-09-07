package com.nothingisland.app.core.cutout

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.graphics.PathParser
import com.nothingisland.app.model.CutoutConfig

/**
 * Raw geometric bounds independent of Android Framework types,
 * enabling pure JVM unit testability.
 */
data class CutoutRawBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * High-precision camera cutout detection module for Nothing Phone & Android devices.
 * Integrates DisplayCutout.cutoutPath (API 31+), AOSP SVG parser (API 28-30),
 * and intelligent heuristics for bounding box fallbacks.
 */
object CameraCutoutDetector {

    /**
     * Primary detection method using live DisplayCutout from an attached Window.
     */
    fun detectFromCutout(
        context: Context,
        cutout: DisplayCutout,
        displayWidth: Int,
        displayHeight: Int
    ): CutoutConfig? {
        val isPortrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        if (!isPortrait) {
            // Nothing Island is designed for top camera in portrait orientation
            return null
        }

        val density = context.resources.displayMetrics.density
        if (density <= 0f) return null

        // 1. Android 12+ (API 31+): Exact vector cutoutPath
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pathBounds = detectFromCutoutPath(cutout)
            if (pathBounds != null) {
                return buildConfigFromRawBounds(pathBounds, displayWidth, density)
            }
        }

        // 2. Android 9-11 (API 28-30): AOSP system configuration SVG
        val svgBounds = parseBuiltInDisplayCutoutSvg(displayWidth, displayHeight, density)
        if (svgBounds != null) {
            return buildConfigFromRawBounds(svgBounds, displayWidth, density)
        }

        // 3. Heuristic fallback based on boundingRectTop or boundingRects
        val topRect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cutout.boundingRectTop
        } else {
            cutout.boundingRects.firstOrNull { it.top == 0 || it.centerY() < displayHeight / 2 }
        }

        if (topRect != null && !topRect.isEmpty) {
            val heuristicBounds = calculateHeuristicBounds(
                top = topRect.top.toFloat(),
                bottom = topRect.bottom.toFloat(),
                left = topRect.left.toFloat(),
                right = topRect.right.toFloat(),
                displayWidth = displayWidth,
                density = density
            )
            return buildConfigFromRawBounds(heuristicBounds, displayWidth, density)
        }

        return null
    }

    /**
     * Secondary detection from Context (Activity, Service or Application).
     */
    fun detectFromContext(context: Context): CutoutConfig? {
        // Try getting cutout from Activity Window decorView if available
        if (context is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val cutout = context.window?.decorView?.rootWindowInsets?.displayCutout
                if (cutout != null) {
                    val dm = context.resources.displayMetrics
                    return detectFromCutout(context, cutout, dm.widthPixels, dm.heightPixels)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return null
                val metrics = wm.currentWindowMetrics
                val cutout = metrics.windowInsets.displayCutout
                val bounds = metrics.bounds
                if (cutout != null) {
                    detectFromCutout(context, cutout, bounds.width(), bounds.height())
                } else {
                    val dm = context.resources.displayMetrics
                    val svgBounds = parseBuiltInDisplayCutoutSvg(dm.widthPixels, dm.heightPixels, dm.density)
                    svgBounds?.let { buildConfigFromRawBounds(it, dm.widthPixels, dm.density) }
                }
            } catch (e: Exception) {
                null
            }
        }

        // Android 9-10 (API 28-29) fallback
        val dm = context.resources.displayMetrics
        val svgBounds = parseBuiltInDisplayCutoutSvg(dm.widthPixels, dm.heightPixels, dm.density)
        return svgBounds?.let { buildConfigFromRawBounds(it, dm.widthPixels, dm.density) }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun detectFromCutoutPath(cutout: DisplayCutout): CutoutRawBounds? {
        val fullPath = cutout.cutoutPath ?: return null
        val topRect = cutout.boundingRectTop

        if (!topRect.isEmpty) {
            val clipPath = Path().apply {
                addRect(RectF(topRect), Path.Direction.CW)
            }
            val topCutoutPath = Path(fullPath)
            if (topCutoutPath.op(clipPath, Path.Op.INTERSECT)) {
                val bounds = RectF()
                topCutoutPath.computeBounds(bounds, true)
                if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
                    return CutoutRawBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
                }
            }
        }

        val bounds = RectF()
        fullPath.computeBounds(bounds, true)
        return if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
            CutoutRawBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        } else null
    }

    private fun parseBuiltInDisplayCutoutSvg(
        displayWidth: Int,
        displayHeight: Int,
        density: Float
    ): CutoutRawBounds? {
        return try {
            val res = Resources.getSystem()
            val resId = res.getIdentifier("config_mainBuiltInDisplayCutout", "string", "android")
            if (resId <= 0) return null
            val spec = res.getString(resId)
            if (spec.isNullOrBlank()) return null
            parseCutoutSvgSpec(spec, displayWidth, displayHeight, density)
        } catch (e: Exception) {
            null
        }
    }

    fun parseCutoutSvgSpec(
        spec: String,
        displayWidth: Int,
        displayHeight: Int,
        density: Float
    ): CutoutRawBounds? {
        return try {
            val inDp = spec.contains("@dp")
            val isRight = spec.contains("@right")
            val isLeft = spec.contains("@left")
            val isBottom = spec.contains("@bottom")
            val isCenterVertical = spec.contains("@center_vertical")

            val cleanSvg = spec
                .replace("@dp", "")
                .replace("@right", "")
                .replace("@left", "")
                .replace("@bottom", "")
                .replace("@center_vertical", "")
                .replace("@cutout", "")
                .replace("@bind_left_cutout", "")
                .replace("@bind_right_cutout", "")
                .trim()

            val path = PathParser.createPathFromPathData(cleanSvg) ?: return null
            val matrix = Matrix()
            if (inDp) {
                matrix.postScale(density, density)
            }

            val offsetX = when {
                isRight -> displayWidth.toFloat()
                isLeft -> 0f
                else -> displayWidth / 2f
            }
            val offsetY = when {
                isBottom -> displayHeight.toFloat()
                isCenterVertical -> displayHeight / 2f
                else -> 0f
            }
            matrix.postTranslate(offsetX, offsetY)
            path.transform(matrix)

            val bounds = RectF()
            path.computeBounds(bounds, true)
            if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
                CutoutRawBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Smart heuristic calculation when only AOSP bounding box is available.
     * Overcomes the AOSP rect.top == 0 expansion issue.
     */
    fun calculateHeuristicBounds(
        top: Float,
        bottom: Float,
        left: Float,
        right: Float,
        displayWidth: Int,
        density: Float
    ): CutoutRawBounds {
        val rectWidthDp = (right - left) / density
        val rectBottomDp = bottom / density
        val topPx = if (top > 0) {
            top
        } else {
            val estimatedDiameterDp = if (rectWidthDp in 20f..42f) rectWidthDp else 28f
            val topMarginDp = ((rectBottomDp - estimatedDiameterDp) / 2f).coerceAtLeast(6f)
            topMarginDp * density
        }

        val diameterPx = if (rectWidthDp in 20f..42f) {
            right - left
        } else {
            28f * density
        }

        val centerXPx = if ((right - left) > 0 && rectWidthDp in 20f..100f) {
            (left + right) / 2f
        } else {
            displayWidth / 2f
        }

        return CutoutRawBounds(
            left = centerXPx - (diameterPx / 2f),
            top = topPx,
            right = centerXPx + (diameterPx / 2f),
            bottom = topPx + diameterPx
        )
    }

    /**
     * Maps raw pixel bounds into calibrated CutoutConfig with DP dimensions.
     */
    fun buildConfigFromRawBounds(
        bounds: CutoutRawBounds,
        displayWidth: Int,
        density: Float
    ): CutoutConfig {
        val diameterPx = bounds.width.coerceAtLeast(bounds.height)
        val topMarginPx = bounds.top
        val centerXPx = bounds.centerX
        val screenCenterXPx = displayWidth / 2f

        val diameterDp = diameterPx / density
        val topMarginDp = topMarginPx / density
        val centerXOffsetDp = (centerXPx - screenCenterXPx) / density

        // Resting pill height: diameter + 6dp for OLED bezel, min 32dp
        val pillHeightDp = (diameterDp + 6f).coerceAtLeast(32f)

        return CutoutConfig(
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
    }
}
