package com.voicesnooze

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * AlarmRingActivity: the fullscreen alarm UI that appears when the alarm fires.
 *
 * Responsibilities:
 * - Display a fullscreen "ALARM" message
 * - Play the ringtone
 * - Listen for voice input (snooze X min / stop)
 * - Handle voice commands via VoiceParser
 * - Release WakeLock acquired by AlarmReceiver
 *
 * Lifecycle:
 * - onCreate(): Set up views, request permissions, initialize SpeechRecognizer
 * - onResume(): Start ringtone, start listening for voice
 * - onPause(): Stop ringtone, release WakeLock
 */
class AlarmRingActivity : ComponentActivity(), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var ringtone: android.media.Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var listeningJob: Job? = null

    private val statusState = mutableStateOf("Listening...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Release WakeLock acquired by AlarmReceiver
        val wakeLockTag = intent.getStringExtra(AlarmReceiver.EXTRA_WAKELOCK_TAG) ?: "voicesnooze:alarm"
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)

        // Fullscreen flags: show on lock screen, turn screen on, etc.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Modern API (Android 8.1+): use Activity-level flags
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // Fallback for older Android (rarely needed but good practice)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Initialize SpeechRecognizer
        // Why check SpeechRecognizer.isRecognitionAvailable()? Some devices lack speech recognition
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
        }

        // Compose UI
        setContent {
            val listeningStateLocal = remember { statusState }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ALARM!",
                    fontSize = 64.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    listeningStateLocal.value,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { stopAlarm() }
                ) {
                    Text("Stop Alarm")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wakeLock?.acquire(3600000) // Hold wake lock for 1 hour max (in case activity stays open)

        // Play ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
        // Play at max volume
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )
        ringtone?.play()

        // Start listening for voice commands
        startListening()
    }

    override fun onPause() {
        super.onPause()
        ringtone?.stop()
        stopListening()
        wakeLock?.release()
    }

    /**
     * Start listening for voice input.
     * Restarts listening every 5 seconds if no speech detected (auto-timeout).
     * Uses lifecycleScope.launch {} so coroutines are tied to Activity lifecycle.
     */
    private fun startListening() {
        if (speechRecognizer == null) {
            statusState.value = "Speech recognition unavailable"
            return
        }

        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            statusState.value = "Microphone permission not granted"
            return
        }

        // Use intent to configure recognizer
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        // Restart listening every 5 seconds if nothing is heard
        // lifecycleScope ensures the job is canceled when Activity is destroyed
        listeningJob = lifecycleScope.launch {
            while (true) {
                try {
                    speechRecognizer?.startListening(intent)
                    // Wait 5 seconds for results
                    delay(5000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during listening", e)
                    delay(1000)
                }
            }
        }

        statusState.value = "Listening for voice..."
    }

    private fun stopListening() {
        listeningJob?.cancel()
        speechRecognizer?.stopListening()
    }

    /**
     * SpeechRecognizer callback: called with recognition results.
     * The recognizer may return partial results before it settles on a final result.
     */
    override fun onResults(results: Bundle?) {
        if (results == null) return

        // Get the list of recognized strings (best match first)
        val matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val recognized = matches[0]
            Log.d(TAG, "Recognized: $recognized")

            // Parse the recognized text into a command
            val command = VoiceParser.parse(recognized)

            when (command) {
                is AlarmCommand.Snooze -> {
                    statusState.value = "Snoozed for ${command.minutes} min"
                    // Schedule a new alarm for N minutes from now
                    val calendar = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MINUTE, command.minutes)
                    }
                    AlarmScheduler.scheduleAlarm(
                        this,
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE)
                    )
                    // Stop ringtone and finish the activity
                    ringtone?.stop()
                    finish()
                }
                is AlarmCommand.Stop -> {
                    statusState.value = "Alarm stopped"
                    stopAlarm()
                }
                is AlarmCommand.Unknown -> {
                    statusState.value = "Didn't catch that. Say 'snooze X minutes' or 'stop'"
                    // Continue listening
                }
            }
        }
    }

    /**
     * Called when the recognizer detects an error.
     * Common errors: ERROR_NO_MATCH (no speech detected), ERROR_NETWORK_TIMEOUT, etc.
     */
    override fun onError(error: Int) {
        val errorMsg = when (error) {
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            else -> "Recognition error: $error"
        }
        Log.e(TAG, errorMsg)
        statusState.value = errorMsg
        // Continue listening (the coroutine will restart it)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun stopAlarm() {
        ringtone?.stop()
        finish()
    }

    /**
     * Handle physical buttons (volume, power) in case user can't reach the screen.
     * Volume down = snooze, volume up = stop.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Snooze for 10 minutes
                val calendar = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.MINUTE, 10)
                }
                AlarmScheduler.scheduleAlarm(
                    this,
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE)
                )
                ringtone?.stop()
                finish()
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                stopAlarm()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        private const val TAG = "AlarmRingActivity"
    }
}
