package com.nothingisland.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.R
import com.nothingisland.app.model.IslandState
import com.nothingisland.app.ui.MainActivity
import com.nothingisland.app.ui.components.NothingIslandRoot
import com.nothingisland.app.ui.theme.NothingIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
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

            setContent {
                NothingIslandTheme {
                    NothingIslandRoot(
                        stateManager = IslandApplication.stateManager,
                        config = IslandApplication.cutoutConfig
                    )
                }
            }
        }

        val initialParams = createLayoutParams(IslandState.Idle)
        windowManager.addView(composeView, initialParams)
    }

    private fun observeState() {
        scope.launch {
            IslandApplication.stateManager.state.collectLatest { state ->
                composeView?.let { view ->
                    val updatedParams = createLayoutParams(state)
                    try {
                        windowManager.updateViewLayout(view, updatedParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun createLayoutParams(state: IslandState): WindowManager.LayoutParams {
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density

        val (wDp, hDp, touchable) = when (state) {
            is IslandState.Idle -> Triple(1, 1, false)
            is IslandState.Compact -> Triple(
                (IslandApplication.cutoutConfig.compactPillWidthDp + 20).toInt(),
                (IslandApplication.cutoutConfig.compactPillHeightDp + IslandApplication.cutoutConfig.cameraTopMarginDp + 10).toInt(),
                true
            )
            is IslandState.Expanded -> Triple(
                (IslandApplication.cutoutConfig.expandedCardWidthDp + 20).toInt(),
                (IslandApplication.cutoutConfig.expandedCardHeightDp + IslandApplication.cutoutConfig.cameraTopMarginDp + 10).toInt(),
                true
            )
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
            x = (IslandApplication.cutoutConfig.cameraCenterXOffsetDp * density).toInt()
            y = 0
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
