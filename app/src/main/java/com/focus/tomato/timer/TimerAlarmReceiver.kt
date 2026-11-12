package com.focus.tomato.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.focus.tomato.data.TimerStore

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        TimerStore.init(context)
        val service = Intent(context, FocusTimerService::class.java)
            .setAction(TimerActions.ACTION_ALARM)
        runCatching { ContextCompat.startForegroundService(context, service) }
    }
}
