package com.nothingisland.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import com.nothingisland.app.IslandApplication

/**
 * Accessibility service that hosts the Dynamic Island overlay via WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY.
 *
 * In Android, TYPE_ACCESSIBILITY_OVERLAY has layer priority 311000, which sits strictly above the System Status Bar (210000).
 * This prevents status bar notification icons from drawing over or cutting into the dynamic island.
 */
class IslandAccessibilityService : AccessibilityService() {

    companion object {
        var isConnected: Boolean = false
            private set
    }

    private var overlayViewHolder: IslandOverlayViewHolder? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        IslandOverlayCoordinator.setAccessibilityActive(true)

        // Setup app launch handler if not already set
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

        // Attach overlay using TYPE_ACCESSIBILITY_OVERLAY (Layer 311000)
        overlayViewHolder = IslandOverlayViewHolder(
            context = this,
            useAccessibilityOverlay = true
        ).apply {
            attach()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: We do not process accessibility events to conserve CPU and battery
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayViewHolder?.onConfigurationChanged(newConfig)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        isConnected = false
        IslandOverlayCoordinator.setAccessibilityActive(false)
        overlayViewHolder?.detach()
        overlayViewHolder = null
    }
}
