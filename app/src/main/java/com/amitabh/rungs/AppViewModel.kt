package com.amitabh.rungs

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app.filesDir)

    // One writer at a time, so saves land on disk in the order they were made.
    private val io = Dispatchers.IO.limitedParallelism(1)

    var data by mutableStateOf(store.load())
        private set

    fun update(change: (AppData) -> AppData) {
        val next = change(data)
        data = next
        viewModelScope.launch(io) { store.save(next) }
    }

    fun startSession() = update { d ->
        val today = LocalDate.now()
        d.copy(inProgress = Session(nextDay(d.sessions), today.toString(), System.currentTimeMillis(), comeback = isComeback(d.sessions, today)))
    }

    /** Logs a set and starts the rest countdown. */
    fun logSet(set: SetLog) {
        update { d -> d.copy(inProgress = d.inProgress?.let { it.copy(sets = it.sets + set) }) }
        RestTimer.start(getApplication(), data.profile.restSeconds)
    }

    /**
     * Logs a strength-test max set at the current level, then either moves to the next level to
     * test or settles the starting level. Tried levels come from the session's own sets.
     */
    fun logTest(m: Movement, value: Int) {
        val p = data.progressOf(m)
        logSet(SetLog(m.id, p.level, value, p.books, test = true))
        val tried = data.inProgress?.sets.orEmpty().filter { it.test && it.movementId == m.id }.mapTo(HashSet()) { it.level }
        setProgress(m, when (val step = testStep(m, p.level, value, tried)) {
            is TestStep.TestAgain -> p.copy(level = step.level)
            is TestStep.Start -> p.copy(level = step.level, tested = true)
        })
    }

    /** Edit (or with null, delete) a logged set by its index in the session. */
    fun editSet(index: Int, value: Int?) = update { d ->
        d.copy(inProgress = d.inProgress?.let { s ->
            s.copy(sets = s.sets.toMutableList().apply { if (value == null) removeAt(index) else set(index, get(index).copy(value = value)) })
        })
    }

    /** Level and books changed mid-workout become the new current position on the ladder. */
    fun setProgress(m: Movement, p: Progress) = update { d -> d.copy(progress = d.progress + (m.id to p)) }

    /** Saves the session if anything was logged; an empty one is just dropped. */
    fun finishSession(keep: Boolean) {
        RestTimer.cancel(getApplication())
        update { d ->
            val s = d.inProgress
            d.copy(inProgress = null, sessions = if (keep && s != null && s.sets.isNotEmpty()) d.sessions + s else d.sessions)
        }
    }

    /** Accept a progression suggestion from the Today screen. */
    fun accept(m: Movement, s: Suggestion, slowTempo: Boolean = false) = update { d ->
        val p = d.progressOf(m)
        val next = when {
            s == Suggestion.LEVEL_UP -> Progress(level = p.level + 1, tested = p.tested)
            slowTempo -> p.copy(slowTempo = true)
            else -> p.copy(books = p.books + 1)
        }
        d.copy(progress = d.progress + (m.id to next))
    }

    fun addWeight(kg: Double) = update { d -> d.copy(weights = withWeight(d.weights, Weight(LocalDate.now().toString(), kg))) }

    /** Writes a backup to a file the user picked. lastExport only moves once the write succeeded. */
    fun export(uri: Uri) = viewModelScope.launch {
        val out = data.copy(lastExport = LocalDate.now().toString())
        val ok = withContext(io) {
            runCatching { getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { Store.export(out, it) } }.isSuccess
        }
        if (ok) update { it.copy(lastExport = out.lastExport) }
        toast(if (ok) "Backup saved" else "Couldn't save the backup")
    }

    /** Replaces everything with a backup file. A bad file changes nothing. */
    fun import(uri: Uri) = viewModelScope.launch {
        val read = withContext(io) {
            runCatching { getApplication<Application>().contentResolver.openInputStream(uri)!!.use { Store.import(it) } }.getOrNull()
        }
        if (read != null) update { read.copy(inProgress = null) }
        toast(if (read != null) "Backup restored: ${read.sessions.size} sessions" else "That file isn't a Rungs backup")
    }

    private fun toast(text: String) = Toast.makeText(getApplication(), text, Toast.LENGTH_SHORT).show()
}
