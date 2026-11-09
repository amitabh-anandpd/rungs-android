package com.amitabh.rungs

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
