package com.example.demo_musicsound.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.demo_musicsound.Audio.BeatPlayer
import com.example.demo_musicsound.Audio.RecorderManager
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.ln
import kotlin.math.min

/**
 * @param rec RecorderManager condiviso.
 * @param currentBeatFile File del beat selezionato (può essere null).
 * @param defaultPlayBeatDuringRec se true abilita di default la riproduzione del beat durante REC.
 */
@Composable
fun RecordScreen(
    rec: RecorderManager,
    currentBeatFile: File? = null,
    defaultPlayBeatDuringRec: Boolean = true
) {
    // --- Stato principale ---
    var recording by remember { mutableStateOf(false) }
    var lastPath by remember { mutableStateOf<String?>(null) }
    var vu by remember { mutableStateOf(0) }

    // Beat
    val beatPlayer = remember { BeatPlayer() }
    var playBeatDuringRec by remember { mutableStateOf(defaultPlayBeatDuringRec) }
    var beatVolume by remember { mutableFloatStateOf(0.9f) }

    // Preview registrazioni (riuso BeatPlayer come player semplice)
    val previewPlayer = remember { BeatPlayer() }
    var previewingFile by remember { mutableStateOf<File?>(null) }

    // Permessi
    val context = LocalContext.current
    var pendingStart by remember { mutableStateOf(false) }

    val micPermission = Manifest.permission.RECORD_AUDIO
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, micPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestMicPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted && pendingStart) {
            startRec(
                rec = rec,
                currentBeatFile = currentBeatFile,
                playBeatDuringRec = playBeatDuringRec,
                beatPlayer = beatPlayer,
                beatVolume = beatVolume,
                setRecording = { recording = it },
                setLastPath = { lastPath = it },
                previewPlayer = previewPlayer,
                setPreviewingFile = { previewingFile = it },
                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
            )
        } else if (!granted) {
            Toast.makeText(context, "Permesso microfono negato", Toast.LENGTH_SHORT).show()
        }
        pendingStart = false
    }

    // Aggiorna VU mentre registri
    LaunchedEffect(recording) {
        if (recording) {
            while (true) {
                vu = rec.maxAmp()
                delay(80)
            }
        } else {
            vu = 0
        }
    }

    // Lista files
    var files by remember { mutableStateOf(rec.listRecordings()) }
    LaunchedEffect(recording, lastPath) { files = rec.listRecordings() }

    // Cleanup alla chiusura della schermata
    DisposableEffect(Unit) {
        onDispose {
            try { if (recording) rec.stop() } catch (_: Exception) {}
            beatPlayer.stop()
            previewPlayer.stop()
        }
    }

    // --- UI ---
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Record", style = MaterialTheme.typography.titleLarge)

        // Card info beat
        Card {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Beat selezionato: ${currentBeatFile?.name ?: "—"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = playBeatDuringRec,
                        onCheckedChange = { playBeatDuringRec = it },
                        enabled = currentBeatFile != null && !recording
                    )
                    Text("Riproduci beat durante REC")
                    Spacer(Modifier.weight(1f))
                    if (currentBeatFile != null) {
                        Text("Vol.")
                        Slider(
                            value = beatVolume,
                            onValueChange = {
                                beatVolume = it
                                beatPlayer.setVolume(beatVolume)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(160.dp),
                            enabled = !recording || beatPlayer.isPlaying()
                        )
                    }
                }
            }
        }

        // VU meter (log-lite)
        val vuProgress = (min(1f, (20f * ln((vu + 1f)) / 100f))).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { vuProgress }, modifier = Modifier.fillMaxWidth())

        // Bottoni REC/STOP
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!recording) {
                        if (!micGranted) {
                            pendingStart = true
                            requestMicPermission.launch(micPermission)
                        } else {
                            startRec(
                                rec = rec,
                                currentBeatFile = currentBeatFile,
                                playBeatDuringRec = playBeatDuringRec,
                                beatPlayer = beatPlayer,
                                beatVolume = beatVolume,
                                setRecording = { recording = it },
                                setLastPath = { lastPath = it },
                                previewPlayer = previewPlayer,
                                setPreviewingFile = { previewingFile = it },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        }
                    } else {
                        stopRec(
                            rec = rec,
                            beatPlayer = beatPlayer,
                            setRecording = { recording = it },
                            setLastPath = { lastPath = it },
                            refreshFiles = { files = rec.listRecordings() },
                            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                enabled = true // Sempre possibile registrare anche senza beat
            ) {
                Text(if (recording) "Stop" else "Rec")
            }

            Text(
                text = "Ultimo file: ${lastPath ?: "—"}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Divider()

        // Lista registrazioni
        Text("Le mie registrazioni", style = MaterialTheme.typography.titleMedium)
        if (files.isEmpty()) {
            Text("Nessuna registrazione ancora.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.absolutePath }) { f ->
                    RecordingRow(
                        file = f,
                        durationText = rec.durationMs(f)?.let { ms -> formatMs(ms) } ?: "—",
                        isPreviewing = (previewingFile == f),
                        onPlay = {
                            if (previewingFile == f) {
                                previewPlayer.stop()
                                previewingFile = null
                            } else {
                                previewPlayer.stop()
                                previewPlayer.play(f, loop = false, volume = 1f)
                                previewingFile = f
                            }
                        },
                        onDelete = {
                            if (previewingFile == f) {
                                previewPlayer.stop()
                                previewingFile = null
                            }
                            rec.delete(f)
                            files = rec.listRecordings()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(
    file: File,
    durationText: String,
    isPreviewing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPreviewing) "Stop" else "Play"
                )
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(durationText, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

/** mm:ss (o hh:mm:ss) */
private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// --- Helper actions (separate for readability) ---
private fun startRec(
    rec: RecorderManager,
    currentBeatFile: File?,
    playBeatDuringRec: Boolean,
    beatPlayer: BeatPlayer,
    beatVolume: Float,
    setRecording: (Boolean) -> Unit,
    setLastPath: (String?) -> Unit,
    previewPlayer: BeatPlayer,
    setPreviewingFile: (File?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        if (playBeatDuringRec && currentBeatFile != null) {
            beatPlayer.play(currentBeatFile, loop = true, volume = beatVolume)
        }
        val out = rec.start()
        setLastPath(out.absolutePath)
        setRecording(true)
        previewPlayer.stop()
        setPreviewingFile(null)
    } catch (t: Throwable) {
        beatPlayer.stop()
        setRecording(false)
        onError("Errore avvio registrazione: ${t.message ?: "sconosciuto"}")
    }
}

private fun stopRec(
    rec: RecorderManager,
    beatPlayer: BeatPlayer,
    setRecording: (Boolean) -> Unit,
    setLastPath: (String?) -> Unit,
    refreshFiles: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val saved = rec.stop()
        setRecording(false)
        beatPlayer.stop()
        refreshFiles()
        setLastPath(saved?.absolutePath)
    } catch (t: Throwable) {
        setRecording(false)
        beatPlayer.stop()
        onError("Errore stop registrazione: ${t.message ?: "sconosciuto"}")
    }
}
