package com.amitabh.rungs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import com.amitabh.rungs.AppData
import com.amitabh.rungs.BodyGoal
import com.amitabh.rungs.Profile
import com.amitabh.rungs.Split
import com.amitabh.rungs.calorieNudge
import com.amitabh.rungs.targetChange
import com.amitabh.rungs.weeklyChange
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(data: AppData, onChange: (Profile) -> Unit, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val p = data.profile
    // Local text so typing "Bed, " isn't trimmed away mid-word; saved on every change.
    var goal by rememberSaveable { mutableStateOf(p.goal) }
    var equipment by rememberSaveable { mutableStateOf(p.equipment.joinToString(", ")) }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            goal, { goal = it; onChange(p.copy(goal = it.trim())) },
            Modifier.fillMaxWidth(), label = { Text("Goal") }, singleLine = true,
        )
        OutlinedTextField(
            equipment,
            { equipment = it; onChange(p.copy(equipment = it.split(',').map(String::trim).filter(String::isNotEmpty))) },
            Modifier.fillMaxWidth(), label = { Text("Equipment, comma separated") },
        )

        Text("Plan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text("How the week is split. Each muscle wants work about twice a week, so pick by how often you train.", color = muted)
        Split.entries.forEach { s -> Choice(s == p.split, s.label, s.hint) { onChange(p.copy(split = s)) } }

        Text("Rest between sets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text("60–90 s is the minimum. Take 90–120 s if your reps drop a lot from set to set. The timer vibrates at the end.", color = muted)
        Stepper(p.restSeconds, 15, "seconds") { onChange(p.copy(restSeconds = it.coerceIn(30, 180))) }

        Text("Body", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text("The training is the same for every body type. What changes is how you eat.", color = muted)
        BodyGoal.entries.forEach { g -> Choice(g == p.bodyGoal, g.label, null) { onChange(p.copy(bodyGoal = g)) } }
        Text(p.bodyGoal.tip, color = muted)
        val last = data.weights.lastOrNull()
        if (last == null) Text("Log your weight in History once a week to get targets here.", color = muted)
        else {
            val want = targetChange(p.bodyGoal, last.kg)
            Text("${last.kg} kg on ${last.date}. Target: ${signed(want.start)} to ${signed(want.endInclusive)} kg a week, " +
                "with ${(last.kg * 1.6).roundToInt()}–${(last.kg * 2.2).roundToInt()} g of protein a day.")
            val trend = weeklyChange(data.weights, LocalDate.now())
            if (trend == null) Text("Weigh in weekly, same time of day. After two weeks your trend shows here.", color = muted)
            else {
                val kcal = calorieNudge(p.bodyGoal, last.kg, trend)
                Text(
                    "Trend: ${signed(trend)} kg a week. " + when {
                        kcal > 0 -> "Too slow: eat about $kcal kcal more a day."
                        kcal < 0 -> "Too fast: eat about ${-kcal} kcal less a day."
                        else -> "On track, keep eating like this."
                    },
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun signed(kg: Double) = "%+.2f".format(kg)

@Composable
private fun Choice(selected: Boolean, label: String, hint: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(label)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
