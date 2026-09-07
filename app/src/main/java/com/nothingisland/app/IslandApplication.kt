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

        fun resetToDefaults() {
            try {
                val prefs = instance.getSharedPreferences("cutout_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_manual_override", false).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val defaults = CutoutConfig.detectFromSystem(instance) ?: CutoutConfig()
            _cutoutConfig.value = defaults
            saveToPrefs(defaults, isManual = false)
        }

        fun autoDetectAndApply(context: Context? = null): CutoutConfig? {
            try {
                val prefs = instance.getSharedPreferences("cutout_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_manual_override", false).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val targetContext = context ?: instance
            val detected = CutoutConfig.detectFromSystem(targetContext) ?: return null
            _cutoutConfig.value = detected
            saveToPrefs(detected, isManual = false)
            return detected
        }

        fun saveManualConfig(config: CutoutConfig) {
            val manualConfig = config.copy(isAutoDetected = false)
            _cutoutConfig.value = manualConfig
            saveToPrefs(manualConfig, isManual = true)
        }

        private fun saveToPrefs(config: CutoutConfig, isManual: Boolean? = null) {
            try {
                val prefs = instance.getSharedPreferences("cutout_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean("is_configured_v2", true)
                    .putFloat("top_margin", config.cameraTopMarginDp)
                    .putFloat("diameter", config.cameraDiameterDp)
                    .putFloat("pill_height", config.compactPillHeightDp)
                    .putFloat("compact_width", config.compactPillWidthDp)
                    .putFloat("center_x", config.cameraCenterXOffsetDp)
                    .putBoolean("is_auto_detected", config.isAutoDetected)
                if (isManual != null) {
                    editor.putBoolean("is_manual_override", isManual)
                }
                editor.apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadFromPrefs(): CutoutConfig {
            return try {
                val prefs = instance.getSharedPreferences("cutout_prefs", Context.MODE_PRIVATE)
                if (!prefs.contains("is_configured_v2")) {
                    val detected = CutoutConfig.detectFromSystem(instance) ?: CutoutConfig()
                    saveToPrefs(detected)
                    return detected
                }
                val topMargin = prefs.getFloat("top_margin", 9f)
                val diameter = prefs.getFloat("diameter", 28f)
                val pillHeight = prefs.getFloat("pill_height", 34f)
                val compactWidth = prefs.getFloat("compact_width", 136f)
                val centerX = prefs.getFloat("center_x", 0f)
                val isAuto = prefs.getBoolean("is_auto_detected", false)
                CutoutConfig(
                    cameraTopMarginDp = topMargin,
                    cameraDiameterDp = diameter,
                    compactPillHeightDp = pillHeight,
                    compactMediaWidthDp = 136f,
                    compactNotifWidthDp = 190f,
                    compactBatteryWidthDp = 100f,
                    compactTimerWidthDp = 130f,
                    compactVolumeWidthDp = 110f,
                    compactPillWidthDp = compactWidth,
                    cameraCenterXOffsetDp = centerX,
                    isAutoDetected = isAuto
                )
            } catch (e: Exception) {
                CutoutConfig.detectFromSystem(instance) ?: CutoutConfig()
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
