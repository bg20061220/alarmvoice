package com.voicesnooze

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.os.Bundle

/**
 * MainActivity: the home screen where users set alarms.
 *
 * Lifecycle:
 * - onCreate(): View setup (called once)
 * - onResume(): Activity becomes visible (permissions, listeners should start here)
 * - onPause(): Activity loses focus (listeners, broadcasts should stop)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent: Compose API. Replaces the old setContentView(R.layout.main_activity) pattern.
        // Now we describe the UI in Kotlin instead of XML.
        setContent {
            AlarmVoiceApp()
        }
    }
}

/**
 * Main composable: the alarm-setting UI.
 * Compose recomposes whenever state changes; each recomposition rebuilds the UI tree.
 * We use mutableStateOf() + remember() to persist state across recompositions.
 */
@Composable
fun AlarmVoiceApp() {
    // Time state: separate hour and minute fields
    val hourState = remember { mutableStateOf("7") }
    val minuteState = remember { mutableStateOf("30") }
    val alarmStatusState = remember { mutableStateOf("No alarm set") }

    // Permission launcher: asks the user for SCHEDULE_EXACT_ALARM (Android 12+)
    // rememberLauncherForActivityResult: remembers the launcher across recompositions
    // so we don't re-register each time the composable rebuilds
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        alarmStatusState.value = if (granted) {
            "Permission granted. Set alarm to proceed."
        } else {
            "Exact alarm permission denied. Alarm may be inaccurate."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VoiceSnooze Alarm") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Alarm Time", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Hour and minute input fields (simpler than a full time picker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour TextField
                TextField(
                    value = hourState.value,
                    onValueChange = { hourState.value = it.take(2) }, // Limit to 2 chars
                    label = { Text("Hour") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text(" : ", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
                // Minute TextField
                TextField(
                    value = minuteState.value,
                    onValueChange = { minuteState.value = it.take(2) },
                    label = { Text("Minute") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Set Alarm Button
            Button(
                onClick = {
                    val hour = hourState.value.toIntOrNull() ?: 7
                    val minute = minuteState.value.toIntOrNull() ?: 30

                    // Clamp to valid ranges
                    val validHour = hour.coerceIn(0, 23)
                    val validMinute = minute.coerceIn(0, 59)

                    // Check permission before scheduling (Android 12+ requires SCHEDULE_EXACT_ALARM)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                MainActivity::class.java as android.content.Context?,
                                Manifest.permission.SCHEDULE_EXACT_ALARM
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // Permission not granted; request it
                            permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                            return@Button
                        }
                    }

                    // Permission granted (or Android < 12); schedule the alarm
                    AlarmScheduler.scheduleAlarm(
                        MainActivity::class.java as android.content.Context? ?: return@Button,
                        validHour,
                        validMinute
                    )
                    alarmStatusState.value = "Alarm set for $validHour:${String.format("%02d", validMinute)}"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Alarm")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel Alarm Button
            Button(
                onClick = {
                    AlarmScheduler.cancelAlarm(MainActivity::class.java as android.content.Context? ?: return@onClick)
                    alarmStatusState.value = "Alarm cancelled"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Alarm")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status display
            Text(alarmStatusState.value, fontSize = 16.sp)
        }
    }
}
