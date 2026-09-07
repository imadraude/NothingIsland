package com.nothingisland.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates overlay ownership between [IslandAccessibilityService] (which renders above the status bar
 * with TYPE_ACCESSIBILITY_OVERLAY at layer 311000) and [IslandOverlayService] (fallback with TYPE_APPLICATION_OVERLAY).
 */
object IslandOverlayCoordinator {
    private val _isAccessibilityActive = MutableStateFlow(false)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    fun setAccessibilityActive(active: Boolean) {
        _isAccessibilityActive.value = active
    }
}
