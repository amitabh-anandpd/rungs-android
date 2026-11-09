package com.amitabh.rungs

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
}
