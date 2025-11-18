package com.example.demo_musicsound.Audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.max

class RecorderManager(private val context: Context) {
    private var mr: MediaRecorder? = null
    private var lastFile: File? = null

    // Renamed to avoid clash with getDir()
    private val recordingsDir: File by lazy {
        File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
    }

    /** Starts audio recording and returns the output file. */
    fun start(): File {
        // Release any previous active recorder if left open
        try { mr?.release() } catch (_: Exception) {}
        mr = null

        val out = File(recordingsDir, "voice_${timeStamp()}.m4a")
        lastFile = out

        val local = MediaRecorder()
        try {
            local.setAudioSource(MediaRecorder.AudioSource.MIC)
            local.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            local.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            local.setAudioEncodingBitRate(128_000)
            local.setAudioSamplingRate(44_100)
            local.setOutputFile(out.absolutePath)
            local.prepare()
            local.start()
            mr = local
            return out
        } catch (t: Throwable) {
            // Clean up on failure
            try { local.reset(); local.release() } catch (_: Exception) {}
            mr = null
            try { if (out.exists()) out.delete() } catch (_: Exception) {}
            throw t
        }
    }

    /** Returns current max amplitude for a VU meter. */
    fun maxAmp(): Int = max(0, mr?.maxAmplitude ?: 0)

    /** Stops recording, releases resources and returns the saved file. */
    fun stop(): File? {
        val f = lastFile
        val local = mr
        mr = null
        return try {
            if (local != null) {
                try { local.stop() } catch (_: IllegalStateException) { /* already stopped */ }
                local.release()
            }
            f
        } catch (_: Throwable) {
            // Ensure release on unexpected errors
            try { local?.release() } catch (_: Exception) {}
            f
        }
    }

    /** Returns the list of recordings sorted by newest first. */
    fun listRecordings(): List<File> =
        recordingsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun delete(file: File): Boolean = file.delete()

    /** Exposes the folder containing all recordings. */
    fun getDir(): File = recordingsDir

    /** Returns audio duration in milliseconds (minSdk 24 compatible). */
    fun durationMs(file: File): Long? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        } catch (_: Exception) {
            null
        } finally {
            try { mmr.release() } catch (_: Exception) {}
        }
    }

    private fun timeStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
