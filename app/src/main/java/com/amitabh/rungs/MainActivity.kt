package com.amitabh.rungs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amitabh.rungs.ui.theme.GlassBackground
import com.amitabh.rungs.ui.theme.glass
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amitabh.rungs.ui.HistoryScreen
import com.amitabh.rungs.ui.LaddersScreen
import com.amitabh.rungs.ui.ProfileScreen
import com.amitabh.rungs.ui.SessionScreen
import com.amitabh.rungs.ui.TodayScreen
import com.amitabh.rungs.ui.theme.RungsTheme

enum class Tab(val label: String) { TODAY("Today"), LADDERS("Ladders"), HISTORY("History"), PROFILE("Profile") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so the status and navigation bar icons are always light.
        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT).let { enableEdgeToEdge(it, it) }
        setContent {
            RungsTheme { GlassBackground {
                val vm: AppViewModel = viewModel()
                var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
                // Asked once, on the first workout: lets the rest countdown show on the lock screen.
                // The timer and vibration work either way.
                val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.startSession() }
                // System file pickers, so a backup can go anywhere: Downloads, Drive, a USB stick.
                val exportTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::export) }
                val importFrom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::import) }
                val export = { exportTo.launch("rungs-backup-${java.time.LocalDate.now()}.json") }
                val start = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.startSession()
                }

                if (vm.data.inProgress != null) {
                    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) { pad ->
                        SessionScreen(vm, Modifier.fillMaxSize().padding(pad))
                    }
                } else Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    bottomBar = {
                        // Text-only tabs in a floating glass pill.
                        Row(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().glass(CircleShape).padding(4.dp)) {
                            Tab.entries.forEach { t ->
                                val on = t == tab
                                Text(
                                    t.label,
                                    Modifier.weight(1f).clip(CircleShape)
                                        .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.Transparent)
                                        .selectable(selected = on, role = Role.Tab) { tab = t }
                                        .padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (on) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                ) { pad ->
                    val mod = Modifier.fillMaxSize().padding(pad)
                    when (tab) {
                        Tab.TODAY -> TodayScreen(
                            vm.data, vm.slot,
                            onSlot = vm::chooseSlot, onTogglePick = vm::togglePick,
                            onAccept = vm::accept, onStart = start, onExport = export, modifier = mod,
                        )
                        Tab.HISTORY -> HistoryScreen(
                            vm.data,
                            onAddWeight = vm::addWeight,
                            onExport = export,
                            onImport = { importFrom.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                            modifier = mod,
                        )
                        Tab.LADDERS -> LaddersScreen(vm.data, onEditLevel = vm::editLevel, onSetProgress = vm::setProgress, modifier = mod)
                        Tab.PROFILE -> ProfileScreen(vm.data, onChange = vm::setProfile, modifier = mod)
                    }
                }
            } }
        }
    }
}
