# VoiceSnooze: An Alarm App Built for Learning

VoiceSnooze is a fully functional Android alarm app that teaches Kotlin and Android fundamentals. Each component was built to teach a specific concept—not just get working code.

## Project Structure

```
alarmvoice/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/voicesnooze/
│   │   │   ├── VoiceParser.kt           # Pure Kotlin: parsing voice commands
│   │   │   ├── AlarmScheduler.kt        # Android: AlarmManager, Intents, PendingIntent
│   │   │   ├── AlarmReceiver.kt         # Android: BroadcastReceiver, WakeLock
│   │   │   ├── MainActivity.kt          # Android: Activity, Compose UI, permissions
│   │   │   └── AlarmRingActivity.kt     # Android: SpeechRecognizer, coroutines, fullscreen
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── values/themes.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── KOTLIN_CONCEPTS.md                   # Comprehensive Kotlin/Android concepts guide
└── README.md (this file)
```

## Key Features

✅ **Alarm Scheduling**: Uses `AlarmManager.setExactAndAllowWhileIdle()` for reliable timing on Android 12+
✅ **Voice Control**: SpeechRecognizer with voice-to-text (no third-party NLP)
✅ **Snooze**: Say "snooze 5 minutes" to reschedule the alarm
✅ **Stop**: Say "stop" to dismiss the alarm
✅ **Fullscreen UI**: Displays on lock screen with Material Design 3
✅ **Kotlin Coroutines**: Non-blocking async voice listening

## Building the Project

### Prerequisites
- Android Studio (latest)
- Android SDK 26+ (API level; this app targets API 34)
- Kotlin 1.9.0+

### Build Steps

1. **Clone the repo**:
   ```bash
   git clone <repo-url>
   cd alarmvoice
   ```

2. **Open in Android Studio**:
   - File → Open → select the `alarmvoice/` directory
   - Android Studio will detect `settings.gradle.kts` and load the project

3. **Sync Gradle**:
   - Android Studio should prompt to sync; if not, Tools → Android → Sync Now

4. **Build**:
   ```bash
   ./gradlew build
   ```

5. **Run on emulator/device**:
   - Connect an Android device or start the emulator
   - Run → Run 'app' (or `./gradlew installDebug && adb shell am start -n com.voicesnooze/.MainActivity`)

## How It Works

### 1. User sets an alarm in MainActivity
- Enters hour and minute
- App requests `SCHEDULE_EXACT_ALARM` permission (Android 12+)
- Calls `AlarmScheduler.scheduleAlarm(hour, minute)`

### 2. AlarmScheduler schedules the alarm
- Creates an Intent for `AlarmReceiver`
- Wraps it in a `PendingIntent`
- Calls `AlarmManager.setExactAndAllowWhileIdle()`
- AlarmManager fires at the exact time, even in Doze mode

### 3. AlarmReceiver wakes the app
- Android broadcasts the alarm Intent
- `AlarmReceiver.onReceive()` is called
- Acquires a WakeLock (CPU stays awake)
- Launches `AlarmRingActivity` with fullscreen flags

### 4. AlarmRingActivity plays alarm + listens for voice
- Displays red "ALARM!" screen
- Plays ringtone at max volume
- Starts `SpeechRecognizer` in a coroutine loop
- Listens for voice input (auto-restarts every 5 seconds)

### 5. VoiceParser handles voice commands
- Receives recognized text (e.g., "snooze 5 minutes")
- Uses regex to parse the command
- Returns `AlarmCommand.Snooze(5)` or `AlarmCommand.Stop`

### 6. AlarmRingActivity acts on the command
- **Snooze**: Schedules a new alarm N minutes from now, stops ringtone, exits
- **Stop**: Stops ringtone, dismisses alarm

## Learning Roadmap

The files are ordered to teach concepts that build on each other:

1. **VoiceParser.kt** → Kotlin fundamentals (data classes, sealed classes, regex, when expressions)
2. **AlarmScheduler.kt** → Android system services (AlarmManager, Intent, PendingIntent)
3. **AlarmReceiver.kt** → BroadcastReceiver, lifecycle, WakeLock
4. **MainActivity.kt** → Activity, Compose UI, permission handling
5. **AlarmRingActivity.kt** → SpeechRecognizer, coroutines, fullscreen intents
6. **AndroidManifest.xml** → App configuration, permission model
7. **build.gradle.kts** → Gradle, dependencies, Kotlin DSL

**Read `KOTLIN_CONCEPTS.md` for a deep dive into each concept.**

## Common Issues & Fixes

### Issue: Alarm doesn't fire
**Cause**: Permission denied or device in Doze mode
**Fix**: 
- Check that `SCHEDULE_EXACT_ALARM` permission is granted (Settings → Apps → VoiceSnooze → Permissions)
- Disable Doze mode on device: `adb shell dumpsys deviceidle disable`

### Issue: Speech recognition never starts
**Cause**: `RECORD_AUDIO` permission not granted
**Fix**: Grant permission in app (MainActivity), then try again

### Issue: AlarmRingActivity doesn't show on lock screen
**Cause**: Device Android version < 8.1 or window flags not set
**Fix**: App has fallback code for older devices; check logcat for errors

### Issue: Gradle sync fails
**Cause**: Kotlin/Compose version mismatch
**Fix**: 
- Delete `.gradle/` and `.idea/` folders
- Re-sync in Android Studio

## Testing

### Manual Testing Checklist
- [ ] App launches without crashing
- [ ] Can set an alarm (set one for 1 minute from now)
- [ ] Alarm fires and displays fullscreen UI
- [ ] Say "snooze 5 minutes" → alarm reschedules
- [ ] Say "stop" → alarm dismisses
- [ ] Volume down on fullscreen → snooze 10 minutes
- [ ] Volume up on fullscreen → stop

### Unit Tests (Optional)
Add to `app/src/test/kotlin/com/voicesnooze/`:
```kotlin
class VoiceParserTest {
    @Test
    fun testSnoozeRegex() {
        val command = VoiceParser.parse("snooze 5 minutes")
        assert(command is AlarmCommand.Snooze)
        assert((command as AlarmCommand.Snooze).minutes == 5)
    }
}
```

Run: `./gradlew test`

## Permissions Required

- `SCHEDULE_EXACT_ALARM` (Android 12+): For exact alarm timing
- `RECORD_AUDIO`: For voice input
- `WAKE_LOCK`: To keep CPU awake during broadcast
- `VIBRATE` (optional): For haptic feedback (not used currently)

All are declared in `AndroidManifest.xml`. `SCHEDULE_EXACT_ALARM` and `RECORD_AUDIO` must be granted at runtime on Android 6.0+.

## Next Steps to Extend the App

1. **Persistent Storage**: Add a database to save multiple alarms (Room/SQLite)
2. **Notifications**: Show a notification when the alarm fires (NotificationCompat)
3. **Background Work**: Use WorkManager for reliable background tasks
4. **Theming**: Add Material3 dynamic color (Android 12+)
5. **Statistics**: Track snooze history and user patterns
6. **Custom Ringtones**: Let users pick their own alarm sound
7. **Tests**: Unit tests for VoiceParser, instrumented tests for UI

## References

- [Android Alarms Documentation](https://developer.android.com/training/scheduling/alarms)
- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [Material Design 3](https://m3.material.io/)

## License

This project is educational. Feel free to fork, modify, and learn!

---

**Key Takeaway**: This app demonstrates that you can build production-quality Android apps with modern Kotlin patterns (coroutines, Compose, sealed classes, extension functions, null safety). Use this as a foundation for your own projects.
