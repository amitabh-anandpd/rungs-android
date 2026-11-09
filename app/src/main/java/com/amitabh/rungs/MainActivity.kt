package com.amitabh.rungs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amitabh.rungs.ui.TodayScreen
import com.amitabh.rungs.ui.theme.RungsTheme

enum class Tab(val label: String) { TODAY("Today"), LADDERS("Ladders"), HISTORY("History"), PROFILE("Profile") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RungsTheme {
                val vm: AppViewModel = viewModel()
                var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        // Text-only tabs, as in the wireframes.
                        NavigationBar {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(selected = t == tab, onClick = { tab = t }, icon = { Text(t.label) })
                            }
                        }
                    },
                ) { pad ->
                    val mod = Modifier.fillMaxSize().padding(pad)
                    when (tab) {
                        Tab.TODAY -> TodayScreen(vm.data, onAccept = vm::accept, onStart = null, modifier = mod)
                        else -> Box(mod, contentAlignment = Alignment.Center) { Text("${tab.label} is coming soon") }
                    }
                }
            }
        }
    }
}
