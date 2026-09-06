package com.nothingisland.app.core

interface MediaActionListener {
    fun play()
    fun pause()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
}
