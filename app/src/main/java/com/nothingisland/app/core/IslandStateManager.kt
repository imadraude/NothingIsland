package com.nothingisland.app.core

import com.nothingisland.app.model.IslandEvent
import com.nothingisland.app.model.IslandState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deep domain engine managing Dynamic Island state transitions, priority arbitration,
 * gesture handling, app launching, and auto-dismissal timers.
 */
class IslandStateManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) {
    private val _state = MutableStateFlow<IslandState>(IslandState.Idle)
    val state: StateFlow<IslandState> = _state.asStateFlow()

    private val _isTransitionSettled = MutableStateFlow(true)
    val isTransitionSettled: StateFlow<Boolean> = _isTransitionSettled.asStateFlow()

    private var currentMedia: IslandEvent.Media? = null
    private var activeTimer: IslandEvent.Timer? = null
    private var lastNotification: IslandEvent.Notification? = null
    private var lastBattery: IslandEvent.Battery? = null

    private var dismissedMediaKey: String? = null
    private var autoDismissJob: Job? = null
    private var mediaActionListener: MediaActionListener? = null

    /**
     * Optional handler to open apps when the compact pill is tapped.
     * Returns true if handled, false to fallback to expansion.
     */
    var appLaunchHandler: ((packageName: String) -> Boolean)? = null

    fun setMediaActionListener(listener: MediaActionListener?) {
        this.mediaActionListener = listener
    }

    fun setTransitionSettled(settled: Boolean) {
        _isTransitionSettled.value = settled
    }

    fun postEvent(event: IslandEvent) {
        when (event) {
            is IslandEvent.Media -> handleMediaEvent(event)
            is IslandEvent.Notification -> handleNotificationEvent(event)
            is IslandEvent.Battery -> handleBatteryEvent(event)
            is IslandEvent.Volume -> handleVolumeEvent(event)
            is IslandEvent.Timer -> handleTimerEvent(event)
        }
    }

    private fun handleMediaEvent(media: IslandEvent.Media) {
        val trackKey = "${media.packageName}:${media.title}"
        if (trackKey != dismissedMediaKey) {
            // New track started or different app -> reset dismiss suppression
            dismissedMediaKey = null
        }

        val isNewTrackOrPlaybackChange = currentMedia == null ||
                currentMedia?.title != media.title ||
                currentMedia?.artist != media.artist ||
                currentMedia?.packageName != media.packageName ||
                (currentMedia?.isPlaying == false && media.isPlaying)

        currentMedia = if (media.isPlaying || media.title.isNotBlank()) media else null

        // If user is currently in expanded mode, maintain expanded view without jumping
        when (val curr = _state.value) {
            is IslandState.Expanded.Media -> {
                _state.value = IslandState.Expanded.Media(media)
            }
            is IslandState.Expanded -> {
                // User is viewing another expanded card (e.g. Timer/Notif) -> don't clobber
            }
            is IslandState.Compact.Media -> {
                if (media.isPlaying) {
                    _state.value = IslandState.Compact.Media(media)
                } else {
                    scheduleAutoDismiss(delayMs = 3000L) {
                        currentMedia = null
                        _state.value = resolveFallbackState()
                    }
                }
            }
            is IslandState.Compact.Notification,
            is IslandState.Compact.Battery,
            is IslandState.Compact.Volume -> {
                if (isNewTrackOrPlaybackChange && media.isPlaying) {
                    // Explicit new track or play action overrides temporary HUD immediately
                    autoDismissJob?.cancel()
                    _state.value = IslandState.Compact.Media(media)
                }
                // Otherwise temporary HUD is showing; let it finish its timer, media will resolve in fallback
            }
            IslandState.Idle -> {
                // Only show if playing and not explicitly dismissed by user
                if (media.isPlaying && trackKey != dismissedMediaKey) {
                    _state.value = IslandState.Compact.Media(media)
                }
            }
            else -> Unit
        }
    }

    private fun handleNotificationEvent(notification: IslandEvent.Notification) {
        lastNotification = notification

        // If currently in Expanded view, do NOT kick user out to a compact pill!
        if (_state.value is IslandState.Expanded) {
            return
        }

        autoDismissJob?.cancel()
        _state.value = IslandState.Compact.Notification(notification)
        scheduleAutoDismiss(delayMs = 4500L) {
            _state.value = resolveFallbackState()
        }
    }

    private fun handleBatteryEvent(battery: IslandEvent.Battery) {
        lastBattery = battery

        // If currently in Expanded view, do not collapse user interaction
        if (_state.value is IslandState.Expanded) {
            return
        }

        autoDismissJob?.cancel()
        _state.value = IslandState.Compact.Battery(battery)
        scheduleAutoDismiss(delayMs = 3500L) {
            _state.value = resolveFallbackState()
        }
    }

    private fun handleVolumeEvent(volume: IslandEvent.Volume) {
        // Do not interrupt expanded state
        if (_state.value is IslandState.Expanded) {
            return
        }

        autoDismissJob?.cancel()
        _state.value = IslandState.Compact.Volume(volume)
        scheduleAutoDismiss(delayMs = 2000L) {
            _state.value = resolveFallbackState()
        }
    }

    private fun handleTimerEvent(timer: IslandEvent.Timer) {
        activeTimer = if (timer.isRunning && timer.remainingSeconds > 0) timer else null
        when (val current = _state.value) {
            is IslandState.Expanded.Timer -> {
                _state.value = IslandState.Expanded.Timer(timer)
            }
            is IslandState.Expanded -> {
                // Another card expanded, keep updated in background
            }
            is IslandState.Compact.Timer -> {
                _state.value = if (activeTimer != null) IslandState.Compact.Timer(timer) else resolveFallbackState()
            }
            IslandState.Idle -> {
                if (activeTimer != null) {
                    _state.value = IslandState.Compact.Timer(timer)
                }
            }
            else -> Unit
        }
    }

    /**
     * User tapped the compact island pill.
     * Primary action: Launch originating app. If no launch handler or fails, expand card.
     */
    fun onPillClicked() {
        when (val current = _state.value) {
            is IslandState.Compact.Media -> {
                val launched = appLaunchHandler?.invoke(current.media.packageName) ?: false
                if (!launched) {
                    expand()
                }
            }
            is IslandState.Compact.Notification -> {
                val launched = appLaunchHandler?.invoke(current.notification.packageName) ?: false
                if (!launched) {
                    expand()
                }
            }
            is IslandState.Compact.Battery -> {
                val launched = appLaunchHandler?.invoke("com.android.settings") ?: false
                if (!launched) {
                    expand()
                }
            }
            is IslandState.Compact.Timer -> {
                val launched = appLaunchHandler?.invoke("com.google.android.deskclock") ?: false
                if (!launched) {
                    expand()
                }
            }
            is IslandState.Expanded -> {
                collapse()
            }
            IslandState.Idle -> {
                currentMedia?.let {
                    dismissedMediaKey = null
                    _state.value = IslandState.Compact.Media(it)
                }
            }
            else -> Unit
        }
    }

    /**
     * User long-pressed the pill or swiped down -> Expand into full interactive card.
     */
    fun onPillLongClicked() {
        expand()
    }

    /**
     * Expand the currently active activity into a rich interactive card.
     */
    fun expand() {
        autoDismissJob?.cancel()
        when (val current = _state.value) {
            is IslandState.Compact.Media -> {
                _state.value = IslandState.Expanded.Media(current.media)
            }
            is IslandState.Compact.Notification -> {
                _state.value = IslandState.Expanded.Notification(current.notification)
            }
            is IslandState.Compact.Battery -> {
                _state.value = IslandState.Expanded.Battery(current.battery)
            }
            is IslandState.Compact.Timer -> {
                _state.value = IslandState.Expanded.Timer(current.timer)
            }
            IslandState.Idle -> {
                currentMedia?.let {
                    _state.value = IslandState.Expanded.Media(it)
                }
            }
            else -> Unit
        }
    }

    /**
     * Collapse back from expanded card to compact pill or idle.
     */
    fun collapse() {
        autoDismissJob?.cancel()
        _state.value = resolveFallbackState()
    }

    /**
     * User swiped to dismiss the island completely.
     * Records dismissal key so the same track doesn't immediately resurrect on position ticks.
     */
    fun onDismissSwiped() {
        autoDismissJob?.cancel()
        currentMedia?.let {
            dismissedMediaKey = "${it.packageName}:${it.title}"
        }
        _state.value = IslandState.Idle
    }

    fun playMedia() = mediaActionListener?.play()
    fun pauseMedia() = mediaActionListener?.pause()
    fun skipNext() = mediaActionListener?.skipToNext()
    fun skipPrevious() = mediaActionListener?.skipToPrevious()
    fun seekMedia(positionMs: Long) = mediaActionListener?.seekTo(positionMs)

    private fun resolveFallbackState(): IslandState {
        val timer = activeTimer
        if (timer != null && timer.isRunning) {
            return IslandState.Compact.Timer(timer)
        }
        val media = currentMedia
        if (media != null && media.isPlaying && "${media.packageName}:${media.title}" != dismissedMediaKey) {
            return IslandState.Compact.Media(media)
        }
        return IslandState.Idle
    }

    private fun scheduleAutoDismiss(delayMs: Long, onTimeout: () -> Unit) {
        autoDismissJob?.cancel()
        autoDismissJob = scope.launch {
            delay(delayMs)
            onTimeout()
        }
    }
}
