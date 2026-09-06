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
 * gesture handling, and auto-dismissal timers.
 */
class IslandStateManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) {
    private val _state = MutableStateFlow<IslandState>(IslandState.Idle)
    val state: StateFlow<IslandState> = _state.asStateFlow()

    private var currentMedia: IslandEvent.Media? = null
    private var activeTimer: IslandEvent.Timer? = null
    private var autoDismissJob: Job? = null
    private var mediaActionListener: MediaActionListener? = null

    fun setMediaActionListener(listener: MediaActionListener?) {
        this.mediaActionListener = listener
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
        currentMedia = if (media.isPlaying || media.title.isNotBlank()) media else null

        // If user is currently looking at expanded or another high-priority item, don't interrupt aggressively
        when (val curr = _state.value) {
            is IslandState.Expanded.Media -> {
                _state.value = IslandState.Expanded.Media(media)
            }
            is IslandState.Compact.Media -> {
                if (media.isPlaying) {
                    _state.value = IslandState.Compact.Media(media)
                } else {
                    scheduleAutoDismiss(delayMs = 3000L) {
                        currentMedia = null
                        resolveFallbackState()
                    }
                }
            }
            IslandState.Idle -> {
                if (media.isPlaying) {
                    _state.value = IslandState.Compact.Media(media)
                }
            }
            else -> {
                // Keep background media updated
            }
        }
    }

    private fun handleNotificationEvent(notification: IslandEvent.Notification) {
        autoDismissJob?.cancel()
        _state.value = IslandState.Compact.Notification(notification)
        scheduleAutoDismiss(delayMs = 4500L) {
            resolveFallbackState()
        }
    }

    private fun handleBatteryEvent(battery: IslandEvent.Battery) {
        // Show battery pill when connected to charger or fast charging
        autoDismissJob?.cancel()
        _state.value = IslandState.Compact.Battery(battery)
        scheduleAutoDismiss(delayMs = 3500L) {
            resolveFallbackState()
        }
    }

    private fun handleVolumeEvent(volume: IslandEvent.Volume) {
        // Only show if not in expanded state
        if (_state.value !is IslandState.Expanded) {
            autoDismissJob?.cancel()
            _state.value = IslandState.Compact.Volume(volume)
            scheduleAutoDismiss(delayMs = 2000L) {
                resolveFallbackState()
            }
        }
    }

    private fun handleTimerEvent(timer: IslandEvent.Timer) {
        activeTimer = if (timer.isRunning && timer.remainingSeconds > 0) timer else null
        if (_state.value is IslandState.Expanded.Timer) {
            _state.value = IslandState.Expanded.Timer(timer)
        } else if (_state.value is IslandState.Compact.Timer || _state.value is IslandState.Idle) {
            _state.value = if (activeTimer != null) IslandState.Compact.Timer(timer) else resolveFallbackState()
        }
    }

    /**
     * User tapped the island pill.
     */
    fun onPillClicked() {
        when (val current = _state.value) {
            is IslandState.Compact.Media -> {
                autoDismissJob?.cancel()
                _state.value = IslandState.Expanded.Media(current.media)
            }
            is IslandState.Compact.Notification -> {
                autoDismissJob?.cancel()
                _state.value = IslandState.Expanded.Notification(current.notification)
            }
            is IslandState.Compact.Battery -> {
                autoDismissJob?.cancel()
                _state.value = IslandState.Expanded.Battery(current.battery)
            }
            is IslandState.Compact.Timer -> {
                autoDismissJob?.cancel()
                _state.value = IslandState.Expanded.Timer(current.timer)
            }
            is IslandState.Expanded -> {
                collapse()
            }
            IslandState.Idle -> {
                // If there is background media, resurrect it
                currentMedia?.let {
                    _state.value = IslandState.Compact.Media(it)
                }
            }
            else -> Unit
        }
    }

    /**
     * User long pressed the island pill.
     */
    fun onPillLongClicked() {
        onPillClicked()
    }

    /**
     * User swiped away or tapped outside to collapse.
     */
    fun collapse() {
        autoDismissJob?.cancel()
        _state.value = resolveFallbackState()
    }

    /**
     * User swiped horizontally to dismiss completely.
     */
    fun onDismissSwiped() {
        autoDismissJob?.cancel()
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
        if (media != null && media.isPlaying) {
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
