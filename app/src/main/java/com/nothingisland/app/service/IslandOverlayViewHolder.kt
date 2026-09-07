package com.nothingisland.app.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.core.cutout.CameraCutoutDetector
import com.nothingisland.app.model.CutoutConfig
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.components.NothingIslandRoot
import com.nothingisland.app.ui.theme.NothingIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class IslandOverlayViewHolder(
    private val context: Context,
    private val useAccessibilityOverlay: Boolean
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var composeView: ComposeView? = null
    var isAttached: Boolean = false
        private set

    fun attach() {
        if (isAttached) return
        try {
            lifecycleOwner.onCreate()
            composeView = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)

                ViewCompat.setOnApplyWindowInsetsListener(this) { _, insetsCompat ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (CameraCutoutDetector.getDeviceHardwareConfig(context) == null) {
                            insetsCompat.toWindowInsets()?.displayCutout?.let { cutout ->
                                val (displayWidth, displayHeight) =
                                    CameraCutoutDetector.getFullDisplaySize(context)
                                CameraCutoutDetector.detectFromCutout(
                                    context = context,
                                    cutout = cutout,
                                    displayWidth = displayWidth,
                                    displayHeight = displayHeight
                                )?.let { detected ->
                                    IslandApplication.applyLiveCutoutDetection(detected)
                                }
                            }
                        }
                    }
                    insetsCompat
                }

                setContent {
                    val config by IslandApplication.cutoutConfigFlow.collectAsState()
                    val state by IslandApplication.stateManager.state.collectAsState()
                    NothingIslandTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (state is IslandState.Expanded) {
                                        Modifier.pointerInput(Unit) {
                                            detectTapGestures(onTap = {
                                                IslandApplication.stateManager.collapse()
                                            })
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NothingIslandRoot(
                                stateManager = IslandApplication.stateManager,
                                config = config,
                                applyHorizontalCutoutOffset = false
                            )
                        }
                    }
                }
            }

            val initialParams = createLayoutParams(IslandState.Idle)
            windowManager.addView(composeView, initialParams)
            composeView?.let(ViewCompat::requestApplyInsets)
            isAttached = true
            observeState()
        } catch (e: Exception) {
            e.printStackTrace()
            detach()
        }
    }

    private fun observeState() {
        scope.launch {
            var previousState: IslandState = IslandState.Idle
            combine(
                IslandApplication.stateManager.state,
                IslandApplication.cutoutConfigFlow
            ) { state, _ -> state }.collectLatest { state ->
                composeView?.let { view ->
                    val config = IslandApplication.cutoutConfig
                    val prevW = getTargetWidthDp(previousState, config)
                    val nextW = getTargetWidthDp(state, config)
                    val isShrinking = (previousState is IslandState.Expanded && state !is IslandState.Expanded) ||
                            (previousState is IslandState.Compact && state is IslandState.Idle) ||
                            (nextW < prevW)

                    if (isShrinking) {
                        try {
                            withTimeoutOrNull(420L) {
                                IslandApplication.stateManager.isTransitionSettled.first { it }
                            }
                        } catch (e: Exception) {
                            delay(380L)
                        }
                    }

                    val updatedParams = createLayoutParams(state)
                    try {
                        windowManager.updateViewLayout(view, updatedParams)
                        ViewCompat.requestApplyInsets(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    previousState = state
                }
            }
        }
    }

    private fun getTargetWidthDp(state: IslandState, config: CutoutConfig): Float {
        return when (state) {
            is IslandState.Idle -> config.cameraDiameterDp
            is IslandState.Compact.Media -> config.compactMediaWidthDp
            is IslandState.Compact.Notification -> config.compactNotifWidthDp
            is IslandState.Compact.Battery -> config.compactBatteryWidthDp
            is IslandState.Compact.Timer -> config.compactTimerWidthDp
            is IslandState.Compact.Volume -> config.compactVolumeWidthDp
            is IslandState.Compact -> config.compactPillWidthDp
            is IslandState.Expanded -> config.expandedCardWidthDp
        }
    }

    private fun createLayoutParams(state: IslandState): WindowManager.LayoutParams {
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val windowType = if (useAccessibilityOverlay) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        if (isLandscape) {
            return WindowManager.LayoutParams(
                1, 1,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 0
            }
        }

        val config = IslandApplication.cutoutConfig
        val density = context.resources.displayMetrics.density

        val (wDp, hDp, touchable) = when (state) {
            is IslandState.Idle -> {
                val windowH = config.cameraTopMarginDp + config.cameraDiameterDp
                Triple(config.cameraDiameterDp.toInt(), windowH.toInt(), false)
            }
            is IslandState.Compact -> {
                val targetW = when (state) {
                    is IslandState.Compact.Notification -> config.compactNotifWidthDp
                    is IslandState.Compact.Battery -> config.compactBatteryWidthDp
                    is IslandState.Compact.Timer -> config.compactTimerWidthDp
                    is IslandState.Compact.Volume -> config.compactVolumeWidthDp
                    is IslandState.Compact.Media -> config.compactMediaWidthDp
                }
                val windowW = (targetW + 40f).toInt()
                val windowH = (config.pillTopMarginDp + config.compactPillHeightDp + 20f).toInt()
                Triple(windowW, windowH, true)
            }
            is IslandState.Expanded -> {
                val windowW = (config.expandedCardWidthDp + 40f).toInt()
                val windowH = (config.pillTopMarginDp + config.expandedCardHeightDp + 24f).toInt()
                Triple(windowW, windowH, true)
            }
        }

        val widthPx = (wDp * density).toInt()
        val heightPx = (hDp * density).toInt()

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (!touchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        return WindowManager.LayoutParams(
            widthPx,
            heightPx,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (state is IslandState.Expanded) 0 else (config.cameraCenterXOffsetDp * density).toInt()
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        composeView?.let { view ->
            val state = IslandApplication.stateManager.state.value
            try {
                windowManager.updateViewLayout(view, createLayoutParams(state))
                ViewCompat.requestApplyInsets(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun detach() {
        if (!isAttached) return
        scope.cancel()
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
        }
        lifecycleOwner.onDestroy()
        isAttached = false
    }
}
