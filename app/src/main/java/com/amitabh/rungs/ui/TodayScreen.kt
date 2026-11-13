package com.amitabh.rungs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amitabh.rungs.AppData
import com.amitabh.rungs.Measure
import com.amitabh.rungs.Movement
import com.amitabh.rungs.Suggestion
import com.amitabh.rungs.backupDue
import com.amitabh.rungs.countingSets
import com.amitabh.rungs.isComeback
import com.amitabh.rungs.lastSets
import com.amitabh.rungs.estimatedMinutes
import com.amitabh.rungs.slotLabel
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
    /** Index into the plan's slots: the queue's next one, or the one the user tapped. */
    slot: Int,
    onSlot: (Int) -> Unit,
    onTogglePick: (String) -> Unit,
    onAccept: (Movement, Suggestion, slowTempo: Boolean) -> Unit,
    onStart: (() -> Unit)?,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val split = data.profile.split
    val days = split.slots[slot]
    val picked = data.sessionMovements(days)
    val others = data.movements.filter { it.day in days && it !in picked }
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
        Text("Next up: ${slotLabel(days)}", style = MaterialTheme.typography.headlineMedium)
        // The queue picks the day; tap another to do that one instead.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            split.slots.forEachIndexed { i, s ->
                FilterChip(selected = i == slot, onClick = { onSlot(i) }, label = { Text(slotLabel(s)) })
            }
            Spacer(Modifier.weight(1f))
            Text("${sessionsThisWeek(data.sessions, today)} this week", color = muted)
        }

        if (suggestRest(data.sessions, today)) Note("Six days in a row. Today is a good rest day; the queue will wait.")
        if (comeback) Note("Welcome back. Targets are about 20% lower for this session.")
        if (backupDue(data.lastExport, data.sessions, today)) Note("Time for a backup.", "Export backup" to onExport)

        picked.forEach { m ->
            MovementCard(data, m, comeback, onAccept, onRemove = { onTogglePick(m.id) })
        }

        Text(
            if (picked.isEmpty()) "Add at least one exercise below."
            else "About ${estimatedMinutes(picked.size, data.profile.restSeconds)} min including warm-up. Missed a day? The queue just waits; there's nothing to catch up.",
            style = MaterialTheme.typography.bodySmall, color = muted,
        )
        Button(onClick = { onStart?.invoke() }, enabled = onStart != null && picked.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Start workout")
        }

        if (others.isNotEmpty()) {
            Text("More exercises", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text("Can't do one of today's? Remove it and add another. Your picks are remembered.", style = MaterialTheme.typography.bodySmall, color = muted)
            others.forEach { m ->
                val level = m.levels[data.progressOf(m).level]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(level.name, fontWeight = FontWeight.Medium)
                        Text(level.cue, style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                    OutlinedButton(onClick = { onTogglePick(m.id) }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String, action: Pair<String, () -> Unit>? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
            action?.let { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
        }
    }
}

@Composable
private fun MovementCard(
    data: AppData,
    m: Movement,
    comeback: Boolean,
    onAccept: (Movement, Suggestion, Boolean) -> Unit,
    onRemove: () -> Unit,
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
            TextButton(onClick = onRemove) { Text("Remove from plan") }
        }
    }
}
