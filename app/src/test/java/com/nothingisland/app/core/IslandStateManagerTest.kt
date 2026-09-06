package com.nothingisland.app.core

import com.nothingisland.app.model.IslandEvent
import com.nothingisland.app.model.IslandState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandStateManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var stateManager: IslandStateManager

    @Before
    fun setup() {
        stateManager = IslandStateManager(scope = testScope)
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(IslandState.Idle, stateManager.state.value)
    }

    @Test
    fun postMediaEvent_whenPlaying_updatesToCompactMedia() {
        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )

        stateManager.postEvent(media)

        val current = stateManager.state.value
        assertTrue(current is IslandState.Compact.Media)
        assertEquals("Starboy", (current as IslandState.Compact.Media).media.title)
    }

    @Test
    fun clickCompactMedia_expandsToExpandedMedia() {
        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media)

        stateManager.onPillClicked()

        val current = stateManager.state.value
        assertTrue(current is IslandState.Expanded.Media)
        assertEquals("The Weeknd", (current as IslandState.Expanded.Media).media.artist)
    }

    @Test
    fun temporaryBatteryEvent_autoDismissesToFallback() = testScope.runTest {
        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media)

        val battery = IslandEvent.Battery(percentage = 85, isCharging = true)
        stateManager.postEvent(battery)

        assertTrue(stateManager.state.value is IslandState.Compact.Battery)

        // Advance past 3500ms auto dismiss
        advanceTimeBy(3600L)

        // Fallback should be playing media
        assertTrue(stateManager.state.value is IslandState.Compact.Media)
    }

    @Test
    fun dismissSwipe_resetsToIdle() {
        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media)

        stateManager.onDismissSwiped()

        assertEquals(IslandState.Idle, stateManager.state.value)
    }
}
