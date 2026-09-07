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
    fun temporaryBatteryEvent_autoDismissesToFallback() = runTest(testDispatcher) {
        stateManager = IslandStateManager(scope = this)
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

        // Advance past 3500ms auto dismiss and run scheduled coroutines
        testScheduler.advanceTimeBy(3600L)
        testScheduler.runCurrent()

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

    @Test
    fun onPillClicked_withAppLaunchHandler_delegatesToHandler() {
        var launchedPkg: String? = null
        stateManager.appLaunchHandler = { pkg ->
            launchedPkg = pkg
            true
        }

        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media)
        stateManager.onPillClicked()

        assertEquals("com.spotify.music", launchedPkg)
        // Pill remains compact because handler handled the app opening
        assertTrue(stateManager.state.value is IslandState.Compact.Media)
    }

    @Test
    fun onPillLongClicked_alwaysExpandsCard() {
        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media)
        stateManager.onPillLongClicked()

        assertTrue(stateManager.state.value is IslandState.Expanded.Media)
    }

    @Test
    fun batteryEvent_whileMediaIsExpanded_doesNotCollapseExpandedCard() {
        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media)
        stateManager.expand()
        assertTrue(stateManager.state.value is IslandState.Expanded.Media)

        // Incoming battery charging event
        val battery = IslandEvent.Battery(percentage = 99, isCharging = true)
        stateManager.postEvent(battery)

        // Must still be expanded media!
        assertTrue(stateManager.state.value is IslandState.Expanded.Media)
    }

    @Test
    fun swipeDismiss_suppressesSameTrack_untilNewTrackStarts() {
        val media1 = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Starboy",
            artist = "The Weeknd",
            isPlaying = true
        )
        stateManager.postEvent(media1)
        assertEquals(IslandState.Compact.Media(media1), stateManager.state.value)

        // User dismisses
        stateManager.onDismissSwiped()
        assertEquals(IslandState.Idle, stateManager.state.value)

        // Playback update for SAME track arrives -> must stay Idle
        stateManager.postEvent(media1.copy(positionMs = 12000L))
        assertEquals(IslandState.Idle, stateManager.state.value)

        // NEW track arrives -> brings back Dynamic Island!
        val media2 = media1.copy(title = "Die For You")
        stateManager.postEvent(media2)
        assertTrue(stateManager.state.value is IslandState.Compact.Media)
        assertEquals("Die For You", (stateManager.state.value as IslandState.Compact.Media).media.title)
    }

    @Test
    fun newTrack_overridesTemporaryHUD() {
        val battery = IslandEvent.Battery(percentage = 85, isCharging = true)
        stateManager.postEvent(battery)
        assertTrue(stateManager.state.value is IslandState.Compact.Battery)

        val media = IslandEvent.Media(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Birds of a Feather",
            artist = "Billie Eilish",
            isPlaying = true
        )
        stateManager.postEvent(media)

        assertTrue(stateManager.state.value is IslandState.Compact.Media)
        assertEquals("Birds of a Feather", (stateManager.state.value as IslandState.Compact.Media).media.title)
    }
}
