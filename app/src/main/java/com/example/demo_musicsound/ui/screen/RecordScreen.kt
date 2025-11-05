package com.example.demo_musicsound.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.demo_musicsound.Audio.RecorderManager
import kotlinx.coroutines.delay
import kotlin.math.ln
import kotlin.math.min
import kotlin.ranges.coerceIn

@Composable
fun RecordScreen(rec: RecorderManager) {
    var recording by remember { mutableStateOf(false) }
    var filePath by remember { mutableStateOf<String?>(null) }
    var vu by remember { mutableStateOf(0) }

    LaunchedEffect(recording) {
        if (recording) while (true) { vu = rec.maxAmp(); delay(100)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recorder")
        LinearProgressIndicator(progress = (min(1f, (20f * ln((vu + 1f)) / 100f)).coerceIn(0f,1f)))
        Button(onClick = {
            if (!recording) { filePath = rec.start().absolutePath; recording = true }
            else { rec.stop(); recording = false }
        }) { Text(if (recording) "Stop" else "Rec") }
        Divider()
        Text("Ultimo file: ${filePath ?: "-"}", maxLines = 2)
    }
}

