package com.example.demo_musicsound.Audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes

class SoundManager(private val context: Context) {

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setAudioAttributes(attrs)
        .setMaxStreams(24)
        .build()

    private val sounds = mutableMapOf<String, Int>() // name -> sampleId
    private val loaded = mutableSetOf<Int>()         // sampleId pronti

    init {
        pool.setOnLoadCompleteListener { _, sid, status ->
            if (status == 0) loaded.add(sid) else Log.e("SoundManager", "Load failed sid=$sid status=$status")
        }
    }

    fun preload(name: String, @RawRes resId: Int) {
        if (name !in sounds) {
            val sid = pool.load(context, resId, 1)
            sounds[name] = sid
        }
    }

    fun play(name: String, gain: Float = 1f, rate: Float = 1f) {
        val sid = sounds[name] ?: return
        if (sid !in loaded) return
        pool.play(sid, gain, gain, 1, 0, rate)
    }

    fun release() = pool.release()
}