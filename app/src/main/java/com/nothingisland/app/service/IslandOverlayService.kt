package com.nothingisland.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.R
import com.nothingisland.app.receiver.BatteryStateReceiver
import com.nothingisland.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandOverlayService : Service() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private var fallbackViewHolder: IslandOverlayViewHolder? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var batteryReceiver: BatteryStateReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        setupAppLaunchHandler()
        startForegroundServiceNotification()
        registerBatteryReceiver()
        observeAccessibilityCoordinator()
    }

    private fun setupAppLaunchHandler() {
        if (IslandApplication.stateManager.appLaunchHandler == null) {
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

    private fun observeAccessibilityCoordinator() {
        scope.launch {
            IslandOverlayCoordinator.isAccessibilityActive.collectLatest { isAccessibilityActive ->
                if (isAccessibilityActive) {
                    // Accessibility overlay is active at layer 311000 (above status bar icons).
                    // Detach fallback overlay to prevent duplicate rendering.
                    fallbackViewHolder?.detach()
                    fallbackViewHolder = null
                } else {
                    // Accessibility service is not active; attach fallback overlay at layer 203800.
                    if (fallbackViewHolder == null) {
                        fallbackViewHolder = IslandOverlayViewHolder(
                            context = this@IslandOverlayService,
                            useAccessibilityOverlay = false
                        ).apply {
                            attach()
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fallbackViewHolder?.onConfigurationChanged(newConfig)
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
        fallbackViewHolder?.detach()
        fallbackViewHolder = null
        super.onDestroy()
    }
}
