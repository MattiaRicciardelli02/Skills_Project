package com.example.demo_musicsound.Audio

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.apply
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.indices

class Sequencer(private var bpm: Int = 120, private val steps: Int = 16) {
    private var job: Job? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> get() = _running

    private val grid: MutableMap<String, SnapshotStateList<Boolean>> = mutableMapOf()

    fun setBpm(v: Int) { bpm = v }
    fun isRunning(): Boolean = _running.value

    fun pattern(resName: String): SnapshotStateList<Boolean> =
        grid.getOrPut(resName) { mutableStateListOf<Boolean>().apply { repeat(steps) { add(false) } } }

    fun toggle(resName: String, i: Int) {
        val p = pattern(resName); p[i] = !p[i]
    }

    fun clear(resNames: List<String>) { resNames.forEach { res -> val p = pattern(res); for (i in p.indices) p[i] = false } }
    fun clearAll() { grid.values.forEach { p -> for (i in p.indices) p[i] = false } }
    fun ensureAll(resNames: List<String>) { resNames.forEach { pattern(it) } }

    fun start(scope: CoroutineScope, sound: SoundManager, onTick: (Int) -> Unit = {}) {
        stop()
        val stepMs = (60000 / bpm) / 4
        job = scope.launch(Dispatchers.Main) {
            _running.value = true
            var i = 0
            while (isActive) {
                grid.forEach { (res, stepsList) -> if (stepsList[i]) sound.play(res) }
                onTick(i)
                i = (i + 1) % (grid.values.firstOrNull()?.size ?: steps)
                delay(stepMs.toLong())
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
        _running.value = false
    }
}