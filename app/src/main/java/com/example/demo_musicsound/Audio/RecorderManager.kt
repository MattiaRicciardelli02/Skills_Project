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

    // Rinominata per evitare clash con getDir()
    private val recordingsDir: File by lazy {
        File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
    }

    /** Avvia la registrazione e ritorna il file di output. */
    fun start(): File {
        // Se qualcosa era rimasto aperto, rilascia
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
            try { local.reset(); local.release() } catch (_: Exception) {}
            mr = null
            try { if (out.exists()) out.delete() } catch (_: Exception) {}
            throw t
        }
    }

    /** Massimo amp istantaneo per VU meter. */
    fun maxAmp(): Int = max(0, mr?.maxAmplitude ?: 0)

    /** Ferma e rilascia. Ritorna il file registrato (se presente). */
    fun stop(): File? {
        val f = lastFile
        val local = mr
        mr = null
        return try {
            if (local != null) {
                try { local.stop() } catch (_: IllegalStateException) { /* già fermo */ }
                local.release()
            }
            f
        } catch (_: Throwable) {
            try { local?.release() } catch (_: Exception) {}
            f
        }
    }

    /** Lista dei file (più recenti prima). */
    fun listRecordings(): List<File> =
        recordingsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun delete(file: File): Boolean = file.delete()

    /** Espone la cartella delle registrazioni. */
    fun getDir(): File = recordingsDir

    /** Durata in ms (compatibile minSdk 24: niente .use) */
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
