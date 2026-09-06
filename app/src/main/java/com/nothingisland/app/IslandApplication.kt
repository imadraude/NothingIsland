package com.nothingisland.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.nothingisland.app.core.IslandStateManager
import com.nothingisland.app.model.CutoutConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IslandApplication : Application() {

    companion object {
        lateinit var instance: IslandApplication
            private set
        val stateManager by lazy { IslandStateManager() }

        private val _cutoutConfig = MutableStateFlow(CutoutConfig())
        val cutoutConfigFlow: StateFlow<CutoutConfig> = _cutoutConfig.asStateFlow()

        var cutoutConfig: CutoutConfig
            get() = _cutoutConfig.value
            set(value) {
                _cutoutConfig.value = value
                saveToPrefs(value)
            }

        fun updateCutout(topMarginDp: Float, diameterDp: Float, centerXOffsetDp: Float) {
            val current = _cutoutConfig.value
            _cutoutConfig.value = current.copy(
                cameraTopMarginDp = topMarginDp,
                cameraDiameterDp = diameterDp,
                cameraCenterXOffsetDp = centerXOffsetDp
            )
        }

        private fun saveToPrefs(config: CutoutConfig) {
            try {
                val prefs = instance.getSharedPreferences("cutout_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putFloat("top_margin", config.cameraTopMarginDp)
                    .putFloat("diameter", config.cameraDiameterDp)
                    .putFloat("compact_width", config.compactPillWidthDp)
                    .putFloat("center_x", config.cameraCenterXOffsetDp)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadFromPrefs(): CutoutConfig {
            return try {
                val prefs = instance.getSharedPreferences("cutout_prefs", Context.MODE_PRIVATE)
                val topMargin = prefs.getFloat("top_margin", 10f)
                val diameter = prefs.getFloat("diameter", 34f)
                val compactWidth = prefs.getFloat("compact_width", 184f)
                val centerX = prefs.getFloat("center_x", 0f)
                CutoutConfig(
                    cameraTopMarginDp = topMargin,
                    cameraDiameterDp = diameter,
                    compactPillWidthDp = compactWidth,
                    cameraCenterXOffsetDp = centerX
                )
            } catch (e: Exception) {
                CutoutConfig()
            }
        }

        const val OVERLAY_CHANNEL_ID = "nothing_island_overlay_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        _cutoutConfig.value = loadFromPrefs()
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
