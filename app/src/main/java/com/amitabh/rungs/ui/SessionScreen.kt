package com.amitabh.rungs.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amitabh.rungs.AppViewModel
import com.amitabh.rungs.Measure
import com.amitabh.rungs.RestTimer
import com.amitabh.rungs.SetLog
import com.amitabh.rungs.countingSets
import com.amitabh.rungs.lastSets
import com.amitabh.rungs.slotLabel
import com.amitabh.rungs.startValue
import com.amitabh.rungs.step
import com.amitabh.rungs.target
import kotlinx.coroutines.delay

private val WarmUp = listOf(
    "Arm circles, 30 s each way",
    "Wrist circles, 30 s",
    "Cat-cow, 10 slow reps",
    "Scapular push-ups, 10",
    "Bodyweight squats, 15",
    "Glute bridges, 10",
    "One easy set of today's first exercise",
)

@Composable
fun SessionScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val data = vm.data
    val session = data.inProgress ?: return
    val movements =
        if (session.movementIds.isEmpty()) data.movements.filter { it.day == session.day }
        else session.movementIds.mapNotNull { id -> data.movements.firstOrNull { it.id == id } }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Derived from the saved sets, so a killed app resumes on the right exercise.
    var warmup by rememberSaveable { mutableStateOf(session.sets.isEmpty()) }
    var index by rememberSaveable {
        mutableIntStateOf(movements.indexOfFirst { m -> session.sets.count { it.movementId == m.id && !it.test } < 3 }.coerceAtLeast(0))
    }
    var confirmExit by remember { mutableStateOf(false) }
    var changeLevel by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    BackHandler { confirmExit = true }

    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(250) }
    }
    val restEnd by RestTimer.endsAt.collectAsState()

    val m = movements.getOrNull(index) ?: return
    val p = data.progressOf(m)
    val level = m.levels[p.level]
    val range = target(level, session.comeback)
    val unit = if (level.measure == Measure.SECONDS) "s" else "reps"
    // Indexes into session.sets, so a tap can edit the right one.
    val logged = session.sets.withIndex().filter { it.value.movementId == m.id && !it.value.test }
    val tests = session.sets.filter { it.movementId == m.id && it.test }
    val testing = !p.tested
    val last = countingSets(lastSets(m.id, data.sessions), p)
    val setNo = logged.size

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${slotLabel(session.days)} · " + if (warmup) "warm-up" else "exercise ${index + 1} of ${movements.size}",
                Modifier.weight(1f), color = muted,
            )
            Text(clock((now - session.startedAt) / 1000), color = muted)
        }

        if (warmup) {
            Text("Warm-up", style = MaterialTheme.typography.headlineSmall)
            Text("About 5 minutes, easy pace.", color = muted)
            WarmUp.forEach { Text("•  $it") }
            Button(onClick = { warmup = false }, Modifier.fillMaxWidth()) { Text("Done, start exercises") }
            TextButton(onClick = { confirmExit = true }) { Text("End workout") }
        } else {
            Text(
                buildString {
                    append("Level ${p.level + 1} of ${m.levels.size} · target ${range.first}–${range.last}")
                    if (level.measure == Measure.SECONDS) append(" s")
                    if (p.books > 0) append(" · ${p.books} book${if (p.books > 1) "s" else ""}")
                    if (p.slowTempo) append(" · 4 s lowering")
                },
                color = muted,
            )
            Text(level.name, style = MaterialTheme.typography.headlineSmall)
            Text(level.cue + " Lower for 2–3 seconds, stop 1–3 reps short of failure.", color = muted)

            tests.forEach { t ->
                val l = m.levels[t.level]
                Text("Test · ${l.name}: ${t.value} " + if (l.measure == Measure.SECONDS) "s" else "reps", color = muted)
            }
            if (tests.isNotEmpty() && !testing) Text("Strength test done, starting on this level.", color = MaterialTheme.colorScheme.primary)

            logged.forEachIndexed { i, (sessionIndex, set) ->
                val delta = last.getOrNull(i)?.let { set.value - it.value }
                Row(
                    Modifier.fillMaxWidth().clickable { editing = sessionIndex }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Set ${i + 1}", Modifier.weight(1f))
                    Text(
                        "${set.value} $unit" + if (set.assist > 0) " + ${set.assist} easier" else "",
                        Modifier.weight(1.5f), fontWeight = FontWeight.Medium,
                    )
                    Text(delta?.let { if (it >= 0) "+$it" else "$it" } ?: "", color = muted)
                }
            }

            if (testing) {
                var max by remember(m.id, p.level) { mutableIntStateOf(level.min) }
                Text("Strength test", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("One set, as many clean ${unit} as you can. Stop when your form breaks. This picks your starting level.", color = muted)
                Stepper(max, step(level), unit) { max = it }
                RestCard(restEnd, now)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.setProgress(m, p.copy(tested = true)) }, Modifier.weight(1f)) { Text("Skip test") }
                    Button(onClick = { vm.logTest(m, max) }, Modifier.weight(1f)) { Text("Log test set") }
                }
            } else {
            // Stepper for the next set, starting one above last time.
            var value by remember(m.id, p.level, setNo) { mutableIntStateOf(startValue(level, last, setNo, session.comeback)) }
            var assist by remember(m.id, p.level, setNo) { mutableIntStateOf(0) }
            Text(
                "Set ${setNo + 1}" + (last.getOrNull(setNo)?.let { " · last time ${it.value}" } ?: ""),
                color = muted,
            )
            Stepper(value, step(level), unit) { value = it }
            // Brief rule 4: short of the target on a new level, finish the set on the easier one.
            if (value < range.first && p.level > 0) {
                Text("Finished with ${m.levels[p.level - 1].name}?", color = muted)
                Stepper(assist, 1, "easier reps") { assist = it }
            }

            RestCard(restEnd, now)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { changeLevel = true }, Modifier.weight(1f)) { Text("Change level") }
                Button(
                    onClick = { vm.logSet(SetLog(m.id, p.level, value, p.books, p.slowTempo, assist)) },
                    Modifier.weight(1f),
                ) { Text("Log set") }
            }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) TextButton(onClick = { index-- }) { Text("‹ Previous") }
                Spacer(Modifier.weight(1f))
                if (index < movements.lastIndex) {
                    val done = setNo >= 3
                    if (done) Button(onClick = { index++ }) { Text("Next exercise ›") }
                    else TextButton(onClick = { index++ }) { Text("Skip to next ›") }
                } else {
                    Button(onClick = { vm.finishSession(keep = true) }) { Text("Finish workout") }
                }
            }
        }
    }

    if (confirmExit) {
        val any = session.sets.isNotEmpty()
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("End workout?") },
            text = { Text(if (any) "Finish saves the sets you've logged. Discard throws them away." else "Nothing logged yet.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; vm.finishSession(keep = true) }) { Text(if (any) "Finish" else "End") }
            },
            dismissButton = {
                Row {
                    if (any) TextButton(onClick = { confirmExit = false; vm.finishSession(keep = false) }) { Text("Discard") }
                    TextButton(onClick = { confirmExit = false }) { Text("Keep going") }
                }
            },
        )
    }

    if (changeLevel) {
        AlertDialog(
            onDismissRequest = { changeLevel = false },
            title = { Text(m.name) },
            text = {
                Column {
                    m.levels.forEachIndexed { i, l ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.setProgress(m, p.copy(level = i)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = i == p.level, onClick = { vm.setProgress(m, p.copy(level = i)) })
                            Text(l.name)
                        }
                    }
                    Text("Books in backpack", Modifier.padding(top = 12.dp), color = muted)
                    Stepper(p.books, 1, "books") { vm.setProgress(m, p.copy(books = it)) }
                }
            },
            confirmButton = { TextButton(onClick = { changeLevel = false }) { Text("Done") } },
        )
    }

    editing?.let { i ->
        val set = session.sets.getOrNull(i) ?: return@let
        var v by remember(i) { mutableIntStateOf(set.value) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit set") },
            text = { Stepper(v, step(m.levels.getOrElse(set.level) { level }), unit) { v = it } },
            confirmButton = { TextButton(onClick = { vm.editSet(i, v); editing = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { vm.editSet(i, null); editing = null }) { Text("Delete") } },
        )
    }
}

@Composable
private fun RestCard(restEnd: Long?, now: Long) {
    val end = restEnd ?: return
    val ctx = LocalContext.current
    val left = ((end - now + 999) / 1000).coerceAtLeast(0)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Rest · phone vibrates at 0:00", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(clock(left), fontSize = 64.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { RestTimer.add(ctx, 15) }) { Text("+15 s") }
                TextButton(onClick = { RestTimer.cancel(ctx) }) { Text("Skip rest") }
            }
        }
    }
}

@Composable
internal fun Stepper(value: Int, step: Int, unit: String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        OutlinedButton(onClick = { onChange((value - step).coerceAtLeast(0)) }, Modifier.size(64.dp)) { Text("−", fontSize = 24.sp) }
        Column(Modifier.widthIn(min = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontSize = 44.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { onChange(value + step) }, Modifier.size(64.dp)) { Text("+", fontSize = 24.sp) }
    }
}

private fun clock(seconds: Long) = "%d:%02d".format(seconds / 60, seconds % 60)
