package com.nothingisland.app.model

import android.graphics.Bitmap

sealed interface IslandEvent {
    data class Media(
        val packageName: String,
        val appName: String,
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,
        val isPlaying: Boolean = false,
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
        val trackId: String = ""
    ) : IslandEvent

    data class Notification(
        val key: String,
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : IslandEvent

    data class Battery(
        val percentage: Int,
        val isCharging: Boolean,
        val isFastCharging: Boolean = false
    ) : IslandEvent

    data class Volume(
        val volume: Int,
        val maxVolume: Int
    ) : IslandEvent

    data class Timer(
        val label: String,
        val remainingSeconds: Int,
        val isRunning: Boolean
    ) : IslandEvent
}
