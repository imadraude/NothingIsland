package com.nothingisland.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.R
import com.nothingisland.app.core.cutout.CameraCutoutDetector
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.receiver.BatteryStateReceiver
import com.nothingisland.app.ui.MainActivity
import com.nothingisland.app.ui.components.NothingIslandRoot
import com.nothingisland.app.ui.theme.NothingIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class IslandOverlayService : Service() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val serviceLifecycleOwner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var batteryReceiver: BatteryStateReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        serviceLifecycleOwner.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupAppLaunchHandler()
        startForegroundServiceNotification()
        setupOverlayView()
        observeState()
        registerBatteryReceiver()
    }

    private fun setupAppLaunchHandler() {
        IslandApplication.stateManager.appLaunchHandler = { packageName ->
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    startActivity(launchIntent)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun registerBatteryReceiver() {
        try {
            val receiver = BatteryStateReceiver()
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(receiver, filter)
            batteryReceiver = receiver
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun setupOverlayView() {
        try {
            composeView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(serviceLifecycleOwner)
                setViewTreeSavedStateRegistryOwner(serviceLifecycleOwner)
                setViewTreeViewModelStoreOwner(serviceLifecycleOwner)

                ViewCompat.setOnApplyWindowInsetsListener(this) { _, insetsCompat ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (CameraCutoutDetector.getDeviceHardwareConfig(this@IslandOverlayService) == null) {
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
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
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
                    val isShrinking = (previousState is IslandState.Expanded && state !is IslandState.Expanded) ||
                            (previousState is IslandState.Compact && state is IslandState.Idle)

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
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
        _isRunning.value = false
        IslandApplication.stateManager.appLaunchHandler = null
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            batteryReceiver = null
        }
        scope.cancel()
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
        }
        serviceLifecycleOwner.onDestroy()
        super.onDestroy()
    }
}
