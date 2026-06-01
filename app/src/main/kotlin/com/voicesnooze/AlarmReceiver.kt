package com.voicesnooze

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

/**
 * BroadcastReceiver: listens for the alarm firing.
 * The AlarmManager broadcasts an Intent to this receiver, even if the app is closed.
 *
 * Lifecycle:
 * 1. AlarmManager fires at the scheduled time
 * 2. Android instantiates AlarmReceiver and calls onReceive()
 * 3. We must finish onReceive() in ~10 seconds or Android kills the process
 * 4. We launch AlarmRingActivity (with fullscreen flags) to handle the UI
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        Log.d(TAG, "Alarm firing!")

        // Acquire a WakeLock to keep the CPU awake while we launch the Activity.
        // Why? The device may be in Doze or screen-off. Without this, the CPU sleeps
        // before we can launch AlarmRingActivity, and the alarm silently fails.
        // We acquire a PARTIAL_LOCK (don't need screen) to minimize battery drain.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "voicesnooze:alarm" // Tag for debugging in adb shell dumpsys power
        ).apply {
            // 10 seconds should be plenty to launch an Activity; if it hangs, battery dies slowly instead of instantly
            acquire(10000) // Timeout in milliseconds
        }

        try {
            // Launch AlarmRingActivity with fullscreen flags
            // Intent.FLAG_NEW_TASK: receivers can't show UI in their process; we need a new task
            val alarmIntent = Intent(context, AlarmRingActivity::class.java).apply {
                // Tell AlarmRingActivity to release the WakeLock when it's ready
                putExtra(EXTRA_WAKELOCK_TAG, "voicesnooze:alarm")
                flags = Intent.FLAG_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(alarmIntent)
        } finally {
            // If we can't launch the Activity for some reason, at least release the WakeLock
            // (finally ensures this runs even if an exception occurs)
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_WAKELOCK_TAG = "wakelock_tag"
    }
}
