package com.voicesnooze

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Extension function: adds alarmManager property to Context.
 * Why? Avoids repeating getSystemService() calls and reads cleaner than context.getSystemService(...) as AlarmManager
 */
val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

object AlarmScheduler {
    private const val ALARM_REQUEST_CODE = 1001

    /**
     * Schedules an alarm for a specific time.
     *
     * @param context App context (needs to be app context, not activity, for reliability)
     * @param hour Hour in 24-hour format (0-23)
     * @param minute Minute (0-59)
     *
     * Why setExactAndAllowWhileIdle?
     * - setExact: Delivers at the exact time (vs setAndAllowWhileIdle which is best-effort)
     * - AllowWhileIdle: Ignores Doze mode (required for alarm clocks on Android 6.0+)
     * - Without this, your alarm fires 15+ min late on modern Android
     */
    fun scheduleAlarm(context: Context, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Create an Intent that will be delivered to AlarmReceiver when the alarm fires
        // ACTION_ALARM tells the receiver this is an alarm event (vs other intents)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.voicesnooze.ALARM_ALERT"
        }

        // PendingIntent: wraps an Intent in a permission token the system can fire later.
        // FLAG_UPDATE_CURRENT: if an alarm already exists with this request code, reuse it (don't create a new PendingIntent)
        // FLAG_IMMUTABLE: (Android 12+) the system can't modify this PendingIntent's extras; prevents security issues
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm
        // The AlarmManager will fire the PendingIntent at the exact time, even if the app is closed/Doze is active
        context.alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, // RTC_WAKEUP: wake the device up at the specified time
            calendar.timeInMillis,
            pendingIntent
        )
    }

    /**
     * Cancels a scheduled alarm.
     * Why? Need this for "delete alarm" or "turn off alarm" in the UI.
     */
    fun cancelAlarm(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.alarmManager.cancel(pendingIntent)
    }
}
