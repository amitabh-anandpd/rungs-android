package com.amitabh.rungs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.amitabh.rungs.AppData
import com.amitabh.rungs.Day
import com.amitabh.rungs.Measure
import com.amitabh.rungs.Movement
import com.amitabh.rungs.Suggestion
import com.amitabh.rungs.backupDue
import com.amitabh.rungs.countingSets
import com.amitabh.rungs.isComeback
import com.amitabh.rungs.lastSets
import com.amitabh.rungs.nextDay
import com.amitabh.rungs.oneSessionAway
import com.amitabh.rungs.sessionsThisWeek
import com.amitabh.rungs.suggestRest
import com.amitabh.rungs.suggestion
import com.amitabh.rungs.target
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    data: AppData,
    onAccept: (Movement, Suggestion, slowTempo: Boolean) -> Unit,
    onStart: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val day = nextDay(data.sessions)
    val comeback = isComeback(data.sessions, today)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} · session ${data.sessions.size + 1}",
            style = MaterialTheme.typography.labelLarge, color = muted,
        )
        Text("Next up: ${day.label}", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val accent = MaterialTheme.colorScheme.primary
            Text(
                buildAnnotatedString {
                    Day.entries.forEachIndexed { i, d ->
                        if (i > 0) append("  →  ")
                        if (d == day) withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(d.label) }
                        else append(d.label)
                    }
                },
                Modifier.weight(1f),
                color = muted,
            )
            Text("${sessionsThisWeek(data.sessions, today)} this week", color = muted)
        }

        if (suggestRest(data.sessions, today)) Note("Six days in a row. Today is a good rest day; the queue will wait.")
        if (comeback) Note("Welcome back. Targets are about 20% lower for this session.")
        if (backupDue(data.lastExport, data.sessions, today)) Note("Time for a backup. Export one from History.")

        data.movements.filter { it.day == day }.forEach { m ->
            MovementCard(data, m, comeback, onAccept)
        }

        Text(
            "About 40 min including warm-up. Missed a day? The queue just waits; there's nothing to catch up.",
            style = MaterialTheme.typography.bodySmall, color = muted,
        )
        Button(onClick = { onStart?.invoke() }, enabled = onStart != null, modifier = Modifier.fillMaxWidth()) {
            Text("Start workout")
        }
    }
}

@Composable
private fun Note(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun MovementCard(
    data: AppData,
    m: Movement,
    comeback: Boolean,
    onAccept: (Movement, Suggestion, Boolean) -> Unit,
) {
    val p = data.progressOf(m)
    val level = m.levels[p.level]
    val range = target(level, comeback)
    val unit = if (level.measure == Measure.SECONDS) " s" else ""
    val last = countingSets(lastSets(m.id, data.sessions), p).map { it.value }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(level.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text("Level ${p.level + 1} of ${m.levels.size}", style = MaterialTheme.typography.labelMedium, color = muted)
            }
            Text(
                buildString {
                    append("3 × ${range.first}–${range.last}$unit")
                    if (p.books > 0) append(" · ${p.books} book${if (p.books > 1) "s" else ""}")
                    if (p.slowTempo) append(" · 4 s lowering")
                    if (last.isNotEmpty()) append(" · last time ${last.joinToString(", ")}")
                },
                style = MaterialTheme.typography.bodyMedium, color = muted,
            )
            if (!data.progressOf(m).tested) {
                Text("First time: one max set to check your level", style = MaterialTheme.typography.bodySmall, color = muted)
            }

            when (suggestion(m, p, data.sessions)) {
                Suggestion.LEVEL_UP -> {
                    Text("Ready for ${m.levels[p.level + 1].name}", color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { onAccept(m, Suggestion.LEVEL_UP, false) }) { Text("Move up") }
                }
                Suggestion.ADD_LOAD -> {
                    Text("Top of the range twice. Make it harder:", color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAccept(m, Suggestion.ADD_LOAD, false) }) { Text("Add a book") }
                        if (!p.slowTempo) OutlinedButton(onClick = { onAccept(m, Suggestion.ADD_LOAD, true) }) { Text("Slow tempo") }
                    }
                }
                null -> if (oneSessionAway(m, p, data.sessions)) {
                    val what = if (p.level < m.levels.lastIndex) "Level up" else "Harder"
                    Text(
                        "$what if you hit ${List(3) { level.max }.joinToString(", ")} today",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
