package com.nothingisland.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nothingisland.app.core.IslandStateManager
import com.nothingisland.app.model.CutoutConfig

class IslandApplication : Application() {

    companion object {
        lateinit var instance: IslandApplication
            private set
        val stateManager by lazy { IslandStateManager() }
        var cutoutConfig = CutoutConfig()
        const val OVERLAY_CHANNEL_ID = "nothing_island_overlay_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "Nothing Island Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Nothing Island active over status bar"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
