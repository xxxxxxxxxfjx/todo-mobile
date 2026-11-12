package com.focus.tomato.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.focus.tomato.data.TimerStatus
import com.focus.tomato.data.TimerStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        TimerStore.init(context)
        val state = TimerStore.state.value
        if (state.status == TimerStatus.RUNNING) {
            val remaining = TimerStore.computeRemaining(state)
            if (remaining > 0) TimerStore.rebase(remaining)
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, FocusTimerService::class.java).setAction(TimerActions.ACTION_RECONCILE)
                )
            }
        }
    }
}
