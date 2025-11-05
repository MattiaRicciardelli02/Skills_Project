package com.example.demo_musicsound.Audio

import android.media.MediaPlayer
import java.io.File

/** Player semplice: può riprodurre un File con o senza loop. */
class BeatPlayer {
    private var mp: MediaPlayer? = null

    fun play(file: File, loop: Boolean = false, volume: Float = 1f) {
        stop()
        mp = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            isLooping = loop
            setVolume(volume, volume)
            prepare()
            start()
        }
    }

    fun setVolume(volume: Float) {
        mp?.setVolume(volume, volume)
    }

    fun isPlaying(): Boolean = mp?.isPlaying == true

    fun stop() {
        try { mp?.stop() } catch (_: Exception) {}
        try { mp?.release() } catch (_: Exception) {}
        mp = null
    }
}
