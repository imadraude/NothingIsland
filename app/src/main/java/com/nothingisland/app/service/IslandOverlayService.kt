package com.nothingisland.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.R
import com.nothingisland.app.core.cutout.CameraCutoutDetector
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.MainActivity
import com.nothingisland.app.ui.components.NothingIslandRoot
import com.nothingisland.app.ui.theme.NothingIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IslandOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val serviceLifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        serviceLifecycleOwner.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        setupOverlayView()
        observeState()
    }

    private fun startForegroundServiceNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, IslandApplication.OVERLAY_CHANNEL_ID)
            .setContentTitle("Nothing Island Active")
            .setContentText("Dynamic Island running over status bar")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun setupOverlayView() {
        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(serviceLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(serviceLifecycleOwner)
            setViewTreeViewModelStoreOwner(serviceLifecycleOwner)

            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insetsCompat ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    insetsCompat.toWindowInsets()?.displayCutout?.let { cutout ->
                        val (displayWidth, displayHeight) =
                            CameraCutoutDetector.getFullDisplaySize(this@IslandOverlayService)
                        CameraCutoutDetector.detectFromCutout(
                            context = this@IslandOverlayService,
                            cutout = cutout,
                            displayWidth = displayWidth,
                            displayHeight = displayHeight
                        )?.let { detected ->
                            IslandApplication.applyLiveCutoutDetection(detected)
                        }
                    }
                }
                insetsCompat
            }

            setContent {
                val config by IslandApplication.cutoutConfigFlow.collectAsState()
                NothingIslandTheme {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NothingIslandRoot(
                            stateManager = IslandApplication.stateManager,
                            config = config
                        )
                    }
                }
            }
        }

        val initialParams = createLayoutParams(IslandState.Idle)
        windowManager.addView(composeView, initialParams)
        composeView?.let(ViewCompat::requestApplyInsets)
    }

    private fun observeState() {
        scope.launch {
            combine(
                IslandApplication.stateManager.state,
                IslandApplication.cutoutConfigFlow
            ) { state, _ -> state }.collectLatest { state ->
                composeView?.let { view ->
                    val updatedParams = createLayoutParams(state)
                    try {
                        windowManager.updateViewLayout(view, updatedParams)
                        ViewCompat.requestApplyInsets(view)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun createLayoutParams(state: IslandState): WindowManager.LayoutParams {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            // In landscape, hide overlay to avoid obstructing apps/media
            return WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
        val density = resources.displayMetrics.density

        val (wDp, hDp, touchable) = when (state) {
            is IslandState.Idle -> Triple(1, 1, false)
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
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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

    override fun onDestroy() {
        scope.cancel()
        composeView?.let {
            windowManager.removeView(it)
            composeView = null
        }
        serviceLifecycleOwner.onDestroy()
        super.onDestroy()
    }
}
