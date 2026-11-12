package com.focus.tomato.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    private const val REQUEST_CODE = 2001

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, TimerAlarmReceiver::class.java).setAction(TimerActions.ACTION_ALARM),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(context: Context, remainingMs: Long) {
        if (remainingMs <= 0) return
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = System.currentTimeMillis() + remainingMs
        val pi = pendingIntent(context)
        try {
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            if (exact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi) }
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pendingIntent(context))
    }
}
