package com.nothingisland.app.model

sealed interface IslandState {
    /**
     * Hidden / collapsed tightly into the camera cutout.
     */
    data object Idle : IslandState

    /**
     * Compact pill showing minimal info flanking the camera cutout.
     */
    sealed interface Compact : IslandState {
        data class Media(
            val media: IslandEvent.Media
        ) : Compact

        data class Notification(
            val notification: IslandEvent.Notification
        ) : Compact

        data class Battery(
            val battery: IslandEvent.Battery
        ) : Compact

        data class Volume(
            val volume: IslandEvent.Volume
        ) : Compact

        data class Timer(
            val timer: IslandEvent.Timer
        ) : Compact
    }

    /**
     * Fully expanded interactive card with rich controls and actions.
     */
    sealed interface Expanded : IslandState {
        data class Media(
            val media: IslandEvent.Media
        ) : Expanded

        data class Notification(
            val notification: IslandEvent.Notification
        ) : Expanded

        data class Battery(
            val battery: IslandEvent.Battery
        ) : Expanded

        data class Timer(
            val timer: IslandEvent.Timer
        ) : Expanded
    }
}
