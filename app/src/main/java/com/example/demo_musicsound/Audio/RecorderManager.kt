package com.example.demo_musicsound.Audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class RecorderManager(private val context: Context) {
    private var mr: MediaRecorder? = null
    private var lastFile: File? = null

    fun start(): File {
        val out = File(context.externalCacheDir, "voice_${System.currentTimeMillis()}.m4a")
        lastFile = out
        mr = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(out.absolutePath)
            prepare(); start()
        }
        return out
    }

    fun maxAmp(): Int = mr?.maxAmplitude ?: 0

    fun stop(): File? = try { mr?.stop(); lastFile } finally { mr?.release(); mr = null }
}