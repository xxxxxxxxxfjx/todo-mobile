package com.focus.tomato.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.focus.tomato.MainActivity
import com.focus.tomato.R
import com.focus.tomato.data.TimerMode
import com.focus.tomato.data.TimerState
import com.focus.tomato.data.TimerStatus
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_TIMER = "focus_timer"
    const val CHANNEL_ALERTS = "focus_alerts"
    const val NOTIF_TIMER = 1001
    const val NOTIF_ALERT = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val timer = NotificationChannel(
            CHANNEL_TIMER,
            "专注计时",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示正在进行的番茄钟倒计时"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "完成提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "专注或休息结束时的提醒"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannels(listOf(timer, alerts))
    }

    fun ongoing(context: Context, state: TimerState, taskTitle: String?): Notification {
        val running = state.status == TimerStatus.RUNNING
        val content = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, content,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleAction = if (running) TimerActions.ACTION_PAUSE else TimerActions.ACTION_RESUME
        val toggleLabel = if (running) "暂停" else "继续"
        val toggleIntent = PendingIntent.getService(
            context, 11,
            Intent(context, FocusTimerService::class.java).setAction(toggleAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipIntent = PendingIntent.getService(
            context, 12,
            Intent(context, FocusTimerService::class.java).setAction(TimerActions.ACTION_SKIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_tomato)
            .setContentTitle(titleFor(state))
            .setContentText(taskTitle ?: "把注意力留给此刻")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, toggleLabel, toggleIntent)
            .addAction(0, "跳过", skipIntent)

        if (running) {
            val remaining = state.remainingMs.coerceAtLeast(0L)
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + remaining)
                .setShowWhen(true)
        } else {
            builder.setContentText("已暂停 · ${formatDuration(state.remainingMs)}")
        }
        return builder.build()
    }

    fun notifyOngoing(context: Context, state: TimerState, taskTitle: String?) {
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_TIMER, ongoing(context, state, taskTitle))
        }
    }

    fun notifyAlert(context: Context, title: String, text: String) {
        val contentIntent = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_tomato)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ALERT, notification)
        }
    }

    fun cancelTimer(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_TIMER)
    }

    private fun titleFor(state: TimerState): String = when (state.mode) {
        TimerMode.FOCUS -> if (state.status == TimerStatus.PAUSED) "专注已暂停" else "正在专注"
        TimerMode.SHORT_BREAK -> "短休息"
        TimerMode.LONG_BREAK -> "长休息"
    }

    fun formatDuration(millis: Long): String {
        val total = (millis.coerceAtLeast(0L) + 999) / 1000
        val m = total / 60
        val s = total % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
