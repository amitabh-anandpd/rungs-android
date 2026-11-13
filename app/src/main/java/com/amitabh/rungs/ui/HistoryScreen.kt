package com.amitabh.rungs.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.amitabh.rungs.ui.theme.GlassCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amitabh.rungs.AppData
import com.amitabh.rungs.Measure
import com.amitabh.rungs.backupDue
import com.amitabh.rungs.history
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Short = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun HistoryScreen(
    data: AppData,
    onAddWeight: (Double) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var movementId by rememberSaveable { mutableStateOf(data.movements.first().id) }
    var confirmImport by remember { mutableStateOf(false) }
    // A restored backup may not have the movement picked before it.
    val m = data.movements.firstOrNull { it.id == movementId } ?: data.movements.first()
    val rows = history(m.id, data.sessions)

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Text("${data.sessions.size} sessions logged", color = muted)

        // Per-exercise history.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data.movements.forEach { mv ->
                FilterChip(selected = mv.id == movementId, onClick = { movementId = mv.id }, label = { Text(mv.name) })
            }
        }
        if (rows.isEmpty()) {
            Text("No sets logged for ${m.name} yet.", color = muted)
        } else {
            val unit = { level: Int -> if (m.levels[level].measure == Measure.SECONDS) "s" else "reps" }
            // Best set per session, oldest first; a level label wherever the level changed.
            val points = rows.take(20).asReversed().map { (_, sets) -> sets.maxBy { it.value } }
            if (points.size >= 2) {
                Text("Best set per session", style = MaterialTheme.typography.titleSmall)
                LineChart(
                    values = points.map { it.value.toFloat() },
                    labels = points.mapIndexed { i, p -> if (i == 0 || p.level != points[i - 1].level) "L${p.level + 1}" else null },
                    description = "Best set per session for ${m.name}: " + points.joinToString { "${it.value} at level ${it.level + 1}" },
                )
            }
            // The list doubles as the chart's table view.
            rows.take(30).forEach { (s, sets) ->
                val lvl = sets.first().level
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.localDate.format(Short), Modifier.weight(0.8f), color = muted)
                    Text(m.levels.getOrNull(lvl)?.name ?: "Level ${lvl + 1}", Modifier.weight(2f))
                    Text(sets.joinToString(", ") { it.value.toString() } + " " + unit(lvl.coerceIn(m.levels.indices)), Modifier.weight(1.4f), fontWeight = FontWeight.Medium)
                }
            }
        }

        // Body weight.
        Text("Body weight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        var kg by rememberSaveable { mutableStateOf("") }
        val parsed = kg.replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..300.0 }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                kg, { kg = it }, Modifier.weight(1f),
                label = { Text("Today, kg") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Button(onClick = { onAddWeight(parsed!!); kg = "" }, enabled = parsed != null) { Text("Save") }
        }
        val weights = data.weights.takeLast(30)
        if (weights.size >= 2) {
            LineChart(
                values = weights.map { it.kg.toFloat() },
                labels = weights.map { null },
                description = "Body weight: " + weights.joinToString { "${it.kg} kg on ${it.date}" },
            )
        }
        data.weights.asReversed().take(5).forEach { w ->
            Row(Modifier.fillMaxWidth()) {
                Text(LocalDate.parse(w.date).format(Short), Modifier.weight(1f), color = muted)
                Text("${w.kg} kg", fontWeight = FontWeight.Medium)
            }
        }

        // Backup.
        Text("Backup", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        val due = backupDue(data.lastExport, data.sessions, LocalDate.now())
        GlassCard(accent = due) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    (data.lastExport?.let { "Last backup ${LocalDate.parse(it).format(Short)}." } ?: "No backup yet.") +
                        if (due) " Save one so a lost phone doesn't lose your progress." else "",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExport) { Text("Export backup") }
                    OutlinedButton(onClick = { confirmImport = true }) { Text("Restore") }
                }
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Restore a backup?") },
            text = { Text("Everything in the app is replaced by the file you pick. Export first if you want to keep what's here.") },
            confirmButton = { TextButton(onClick = { confirmImport = false; onImport() }) { Text("Pick file") } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancel") } },
        )
    }
}

/**
 * Single-series line chart: 2dp line, 8dp markers with a surface ring, min/max on the y axis,
 * optional text label above a point. No touch tooltip; the list under it carries every value.
 */
@Composable
private fun LineChart(values: List<Float>, labels: List<String?>, description: String) {
    val line = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.background
    val grid = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 11.sp, color = ink)
    val lo = values.min()
    val hi = values.max().let { if (it == lo) lo + 1 else it }

    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = description }) {
        val axis = 32.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val left = axis
        val right = size.width - 8.dp.toPx()
        fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)
        fun x(i: Int) = left + i * (right - left) / (values.size - 1).coerceAtLeast(1)

        listOf(lo, hi).forEach { v ->
            drawLine(grid, Offset(left, y(v)), Offset(right, y(v)), 1.dp.toPx())
            val t = measurer.measure(fmt(v), style)
            drawText(t, topLeft = Offset(axis - t.size.width - 6.dp.toPx(), y(v) - t.size.height / 2))
        }
        for (i in 1 until values.size) {
            drawLine(line, Offset(x(i - 1), y(values[i - 1])), Offset(x(i), y(values[i])), 2.dp.toPx())
        }
        values.forEachIndexed { i, v ->
            val c = Offset(x(i), y(v))
            drawCircle(surface, 6.dp.toPx(), c)
            drawCircle(line, 4.dp.toPx(), c)
            labels.getOrNull(i)?.let {
                val t = measurer.measure(it, style)
                drawText(t, topLeft = Offset((c.x - t.size.width / 2).coerceIn(0f, size.width - t.size.width), c.y - t.size.height - 6.dp.toPx()))
            }
        }
    }
}

private fun fmt(v: Float) = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)
