package com.nothingisland.app.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.core.MediaActionListener
import com.nothingisland.app.model.IslandEvent

class IslandNotificationListener : NotificationListenerService(), MediaActionListener {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaFromController(activeController)
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        handleActiveSessions(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        IslandApplication.stateManager.setMediaActionListener(this)
        setupMediaSessionManager()
    }

    override fun onListenerDisconnected() {
        IslandApplication.stateManager.setMediaActionListener(null)
        mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        super.onListenerDisconnected()
    }

    private fun setupMediaSessionManager() {
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val componentName = ComponentName(this, IslandNotificationListener::class.java)
        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            handleActiveSessions(controllers)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun handleActiveSessions(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controllers?.firstOrNull()
        activeController?.registerCallback(controllerCallback)
        updateMediaFromController(activeController)
    }

    private fun updateMediaFromController(controller: MediaController?) {
        if (controller == null) return

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L

        val appName = try {
            val pm = packageManager
            val info = pm.getApplicationInfo(controller.packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            controller.packageName
        }

        IslandApplication.stateManager.postEvent(
            IslandEvent.Media(
                packageName = controller.packageName,
                appName = appName,
                title = title,
                artist = artist,
                albumArt = albumArt,
                isPlaying = isPlaying,
                durationMs = duration,
                positionMs = position
            )
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.isOngoing) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        IslandApplication.stateManager.postEvent(
            IslandEvent.Notification(
                key = sbn.key,
                packageName = sbn.packageName,
                title = title,
                text = text
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // MediaActionListener implementation
    override fun play() {
        activeController?.transportControls?.play()
    }

    override fun pause() {
        activeController?.transportControls?.pause()
    }

    override fun skipToNext() {
        activeController?.transportControls?.skipToNext()
    }

    override fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    override fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs)
    }
}
