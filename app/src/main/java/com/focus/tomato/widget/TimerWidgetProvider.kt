package com.focus.tomato.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focus.tomato.MainActivity
import com.focus.tomato.R
import com.focus.tomato.TomatoApp
import com.focus.tomato.data.TimerMode
import com.focus.tomato.data.TimerStatus
import com.focus.tomato.data.TimerStore
import com.focus.tomato.timer.FocusTimerService
import com.focus.tomato.timer.TimerActions
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.focus.tomato.widget.TOGGLE"
        private const val REQ_OPEN = 3001
        private const val REQ_TOGGLE = 3002

        fun updateAll(context: Context) {
            TimerStore.init(context)
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val state = TimerStore.snapshot()
            val remaining = TimerStore.computeRemaining(state)
            val taskTitle = state.taskId?.let { id ->
                runBlocking(Dispatchers.IO) {
                    TomatoApp.instance.database.taskDao().getById(id)?.title
                }
            }
            val modeLabel = when (state.mode) {
                TimerMode.FOCUS -> "专注"
                TimerMode.SHORT_BREAK -> "短休息"
                TimerMode.LONG_BREAK -> "长休息"
            }
            val actionLabel = when (state.status) {
                TimerStatus.RUNNING -> "暂停"
                TimerStatus.PAUSED -> "继续"
                TimerStatus.IDLE -> if (state.mode == TimerMode.FOCUS) "开始专注" else "开始休息"
            }
            val views = RemoteViews(context.packageName, R.layout.widget_timer).apply {
                setTextViewText(R.id.widget_mode, "番茄专注 · $modeLabel")
                setTextViewText(R.id.widget_time, formatTime(remaining))
                setTextViewText(
                    R.id.widget_task,
                    taskTitle ?: if (state.status == TimerStatus.IDLE) "今天想专注什么？" else "把注意力留给此刻"
                )
                setTextViewText(R.id.widget_action, actionLabel)
                setOnClickPendingIntent(
                    R.id.widget_action,
                    PendingIntent.getBroadcast(
                        context,
                        REQ_TOGGLE,
                        Intent(context, TimerWidgetProvider::class.java).setAction(ACTION_TOGGLE),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                setOnClickPendingIntent(
                    R.id.widget_time,
                    PendingIntent.getActivity(
                        context,
                        REQ_OPEN,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun formatTime(millis: Long): String {
            val total = (millis.coerceAtLeast(0L) + 999) / 1000
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) {
                String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", m, s)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        TimerStore.init(context)
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_TOGGLE) {
            TimerStore.init(context)
            val state = TimerStore.state.value
            when (state.status) {
                TimerStatus.RUNNING -> FocusTimerService.command(context, TimerActions.ACTION_PAUSE)
                TimerStatus.PAUSED -> FocusTimerService.command(context, TimerActions.ACTION_RESUME)
                TimerStatus.IDLE -> FocusTimerService.start(context, state.mode, state.taskId, 0)
            }
            updateAll(context)
        }
    }
}
