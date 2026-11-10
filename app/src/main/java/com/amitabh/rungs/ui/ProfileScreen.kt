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
import com.amitabh.rungs.AppData
import com.amitabh.rungs.Profile

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

        Text("Rest between sets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text("The brief says 60–90 s. The timer vibrates at the end.", color = muted)
        Stepper(p.restSeconds, 15, "seconds") { onChange(p.copy(restSeconds = it.coerceIn(30, 180))) }

        Text("Body weight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            data.weights.lastOrNull()?.let { "${it.kg} kg on ${it.date}. Log new weigh-ins in History." }
                ?: "Not logged yet. Add it in History.",
            color = muted,
        )
    }
}
