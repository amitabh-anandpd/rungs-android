package com.amitabh.rungs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amitabh.rungs.AppData
import com.amitabh.rungs.Day
import com.amitabh.rungs.Level
import com.amitabh.rungs.Measure
import com.amitabh.rungs.Movement
import com.amitabh.rungs.Progress
import com.amitabh.rungs.step

@Composable
fun LaddersScreen(
    data: AppData,
    onEditLevel: (Movement, Int, Level) -> Unit,
    onSetProgress: (Movement, Progress) -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var editing by remember { mutableStateOf<Pair<Movement, Int>?>(null) }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ladders", style = MaterialTheme.typography.headlineMedium)
        Text("Easiest at the top. Tap a level to edit its cue or target.", color = muted)

        Day.entries.forEach { day ->
            Text(day.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            data.movements.filter { it.day == day }.forEach { m ->
                val p = data.progressOf(m)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (p.tested) "Level ${p.level + 1} of ${m.levels.size}" else "Not tested yet",
                                color = muted, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        m.levels.forEachIndexed { i, l ->
                            val current = i == p.level
                            Column(Modifier.fillMaxWidth().clickable { editing = m to i }.padding(vertical = 4.dp)) {
                                Row {
                                    Text(
                                        (if (current) "▶ " else "") + "${i + 1}. ${l.name}",
                                        Modifier.weight(1f),
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                        color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text("${l.min}–${l.max}${if (l.measure == Measure.SECONDS) " s" else ""}", color = muted)
                                }
                                Text(l.cue, style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                        }
                        if (p.tested) {
                            TextButton(onClick = { onSetProgress(m, p.copy(tested = false)) }) { Text("Retest next session") }
                        }
                    }
                }
            }
        }
    }

    editing?.let { (m, i) ->
        val l = m.levels[i]
        val p = data.progressOf(m)
        var name by remember(m.id, i) { mutableStateOf(l.name) }
        var cue by remember(m.id, i) { mutableStateOf(l.cue) }
        var min by remember(m.id, i) { mutableIntStateOf(l.min) }
        var max by remember(m.id, i) { mutableIntStateOf(l.max) }
        val unit = if (l.measure == Measure.SECONDS) "s" else "reps"
        val s = step(l)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(m.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(cue, { cue = it }, label = { Text("Form cue") })
                    // Range stays valid: min at least one step below max.
                    Text("Target low", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Stepper(min, s, unit) { min = it.coerceIn(1, max - s) }
                    Text("Target high", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Stepper(max, s, unit) { max = it.coerceAtLeast(min + s) }
                    if (i != p.level) {
                        TextButton(onClick = { onSetProgress(m, p.copy(level = i)); editing = null }) { Text("Train at this level") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onEditLevel(m, i, l.copy(name = name.trim(), cue = cue.trim(), min = min, max = max)); editing = null },
                    enabled = name.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
