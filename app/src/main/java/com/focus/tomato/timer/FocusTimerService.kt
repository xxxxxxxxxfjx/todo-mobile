package com.focus.tomato.timer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.focus.tomato.TomatoApp
import com.focus.tomato.data.AppSettings
import com.focus.tomato.data.SessionEntity
import com.focus.tomato.data.SessionStatus
import com.focus.tomato.data.SessionType
import com.focus.tomato.data.TimerMode
import com.focus.tomato.data.TimerStatus
import com.focus.tomato.data.TimerStore
import com.focus.tomato.data.defaultSeconds
import com.focus.tomato.widget.TimerWidgetProvider
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FocusTimerService : Service() {

    companion object {
        fun start(context: android.content.Context, mode: TimerMode, taskId: Long?, seconds: Int) {
            val intent = Intent(context, FocusTimerService::class.java)
                .setAction(TimerActions.ACTION_START)
                .putExtra(TimerActions.EXTRA_MODE, mode.name)
                .putExtra(TimerActions.EXTRA_TASK_ID, taskId ?: -1L)
                .putExtra(TimerActions.EXTRA_SECONDS, seconds)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun command(context: android.content.Context, action: String) {
            val intent = Intent(context, FocusTimerService::class.java).setAction(action)
            runCatching { context.startService(intent) }
        }

        fun reconcile(context: android.content.Context) {
            val intent = Intent(context, FocusTimerService::class.java)
                .setAction(TimerActions.ACTION_RECONCILE)
            runCatching { androidx.core.content.ContextCompat.startForegroundService(context, intent) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var finishing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TimerStore.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: TimerActions.ACTION_RECONCILE
        ensureForeground()
        when (action) {
            TimerActions.ACTION_START -> handleStart(intent)
            TimerActions.ACTION_PAUSE -> handlePause()
            TimerActions.ACTION_RESUME -> handleResume()
            TimerActions.ACTION_RESET -> handleReset()
            TimerActions.ACTION_SKIP -> handleSkip()
            TimerActions.ACTION_EXTEND -> handleExtend(intent?.getIntExtra(TimerActions.EXTRA_SECONDS, 300) ?: 300)
            TimerActions.ACTION_ALARM -> handleAlarm()
            else -> handleReconcile()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ actions

    private fun handleStart(intent: Intent?) {
        val mode = runCatching {
            TimerMode.valueOf(intent?.getStringExtra(TimerActions.EXTRA_MODE) ?: TimerMode.FOCUS.name)
        }.getOrDefault(TimerMode.FOCUS)
        val taskId = intent?.getLongExtra(TimerActions.EXTRA_TASK_ID, -1L)?.takeIf { it > 0 }
        val settings = readSettings()
        val secondsExtra = intent?.getIntExtra(TimerActions.EXTRA_SECONDS, 0) ?: 0
        val seconds = if (secondsExtra > 0) secondsExtra else plannedFor(mode, settings)

        val current = TimerStore.state.value
        if (current.status == TimerStatus.RUNNING) {
            updateNotification()
            return
        }
        if (current.status == TimerStatus.PAUSED) abandonCurrentSession()
        if (current.sessionId != null) TimerStore.updateSessionId(null)

        startSession(mode, taskId, seconds, current.focusInCycle)
    }

    private fun handlePause() {
        if (TimerStore.state.value.status != TimerStatus.RUNNING) return
        TimerStore.pause()
        tickJob?.cancel()
        AlarmScheduler.cancel(this)
        updateNotification()
    }

    private fun handleResume() {
        if (TimerStore.state.value.status != TimerStatus.PAUSED) return
        TimerStore.resume()
        startTicker()
        AlarmScheduler.schedule(this, TimerStore.computeRemaining())
        updateNotification()
    }

    private fun handleReset() {
        abandonCurrentSession()
        val state = TimerStore.state.value
        val planned = plannedFor(state.mode, readSettings())
        TimerStore.stopToIdle(state.mode, planned, state.focusInCycle)
        stopAndSelf()
    }

    private fun handleSkip() {
        val state = TimerStore.state.value
        abandonCurrentSession()
        val nextMode = if (state.mode == TimerMode.FOCUS) TimerMode.SHORT_BREAK else TimerMode.FOCUS
        val settings = readSettings()
        val planned = plannedFor(nextMode, settings)
        if (settings.autoStartNext) {
            startSession(nextMode, if (nextMode == TimerMode.FOCUS) state.taskId else null, planned, state.focusInCycle)
        } else {
            TimerStore.stopToIdle(nextMode, planned, state.focusInCycle)
            stopAndSelf()
        }
    }

    private fun handleExtend(seconds: Int) {
        val state = TimerStore.state.value
        if (state.status == TimerStatus.IDLE) return
        TimerStore.extend(seconds.coerceIn(60, 3600))
        if (TimerStore.state.value.status == TimerStatus.RUNNING) {
            AlarmScheduler.schedule(this, TimerStore.computeRemaining())
        }
        updateNotification()
    }

    private fun handleAlarm() {
        val state = TimerStore.state.value
        if (state.status != TimerStatus.RUNNING) {
            stopAndSelf()
            return
        }
        val remaining = TimerStore.computeRemaining(state)
        if (remaining <= 2_000L) {
            completeCurrentPhase()
        } else {
            TimerStore.rebase(remaining)
            AlarmScheduler.schedule(this, remaining)
            startTicker()
        }
    }

    private fun handleReconcile() {
        val state = TimerStore.state.value
        when (state.status) {
            TimerStatus.RUNNING -> {
                val remaining = TimerStore.computeRemaining(state)
                if (remaining <= 0L) {
                    completeCurrentPhase()
                } else {
                    TimerStore.rebase(remaining)
                    AlarmScheduler.schedule(this, remaining)
                    startTicker()
                    updateNotification()
                }
            }
            TimerStatus.PAUSED -> updateNotification()
            TimerStatus.IDLE -> {
                abandonOrphanSessions()
                stopAndSelf()
            }
        }
    }

    // ------------------------------------------------------------------ timer core

    private fun startSession(mode: TimerMode, taskId: Long?, seconds: Int, cycle: Int) {
        val planned = seconds.coerceIn(60, 4 * 60 * 60)
        val startedAt = System.currentTimeMillis()
        val sessionId = runBlocking(Dispatchers.IO) {
            TomatoApp.instance.database.sessionDao().insert(
                SessionEntity(
                    taskId = taskId,
                    type = mode.toSessionType(),
                    plannedSeconds = planned,
                    startedAt = startedAt,
                    dayKey = LocalDate.now().toString(),
                    status = SessionStatus.RUNNING
                )
            )
        }
        finishing = false
        TimerStore.markRunning(mode, planned, taskId, sessionId, startedAt, cycle)
        startTicker()
        AlarmScheduler.schedule(this, planned * 1000L)
        updateNotification()
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var ticks = 0
            while (isActive) {
                val state = TimerStore.state.value
                if (state.status != TimerStatus.RUNNING) break
                val remaining = TimerStore.computeRemaining(state)
                TimerStore.setRemaining(remaining)
                if (ticks++ % 30 == 0) TimerWidgetProvider.updateAll(this@FocusTimerService)
                if (remaining <= 0L) {
                    completeCurrentPhase()
                    break
                }
                delay(500)
            }
        }
    }

    private fun completeCurrentPhase() {
        if (finishing) return
        val state = TimerStore.state.value
        if (state.status != TimerStatus.RUNNING) return
        finishing = true
        tickJob?.cancel()

        val endedAt = System.currentTimeMillis()
        runBlocking(Dispatchers.IO) {
            state.sessionId?.let { id ->
                TomatoApp.instance.database.sessionDao().finish(
                    id = id,
                    endedAt = endedAt,
                    actualSeconds = state.plannedSeconds,
                    status = SessionStatus.COMPLETED
                )
            }
            if (state.mode == TimerMode.FOCUS) {
                state.taskId?.let { taskId ->
                    TomatoApp.instance.database.taskDao().incrementCompletedPomodoros(taskId)
                }
            }
        }

        val settings = readSettings()
        if (state.mode == TimerMode.FOCUS) {
            NotificationHelper.notifyAlert(this, "专注完成 🍅", "完成 1 个番茄，休息一下，给大脑一点空白。")
        } else {
            NotificationHelper.notifyAlert(this, "休息结束", "回来继续下一段专注吧。")
        }
        playAlert(settings)

        val nextMode: TimerMode
        val nextCycle: Int
        if (state.mode == TimerMode.FOCUS) {
            val completed = state.focusInCycle + 1
            val interval = settings.longBreakInterval.coerceIn(1, 10)
            if (completed >= interval) {
                nextMode = TimerMode.LONG_BREAK
                nextCycle = 0
            } else {
                nextMode = TimerMode.SHORT_BREAK
                nextCycle = completed
            }
        } else {
            nextMode = TimerMode.FOCUS
            nextCycle = state.focusInCycle
        }

        val nextPlanned = plannedFor(nextMode, settings)
        TimerStore.stopToIdle(nextMode, nextPlanned, nextCycle)
        AlarmScheduler.cancel(this)

        if (settings.autoStartNext) {
            startSession(nextMode, if (nextMode == TimerMode.FOCUS) state.taskId else null, nextPlanned, nextCycle)
        } else {
            stopAndSelf()
        }
        finishing = false
    }

    private fun abandonCurrentSession() {
        val state = TimerStore.state.value
        val sessionId = state.sessionId ?: return
        val elapsed = when (state.status) {
            TimerStatus.RUNNING -> (state.plannedSeconds - TimerStore.computeRemaining(state) / 1000).toInt()
            else -> (state.plannedSeconds - state.remainingMs / 1000).toInt()
        }
        runBlocking(Dispatchers.IO) {
            TomatoApp.instance.database.sessionDao().finish(
                id = sessionId,
                endedAt = System.currentTimeMillis(),
                actualSeconds = elapsed.coerceAtLeast(0),
                status = SessionStatus.ABANDONED
            )
        }
        TimerStore.updateSessionId(null)
    }

    private fun abandonOrphanSessions() {
        runBlocking(Dispatchers.IO) {
            val orphan = TomatoApp.instance.database.sessionDao().latestRunning()
            if (orphan != null) {
                TomatoApp.instance.database.sessionDao().finish(
                    orphan.id,
                    System.currentTimeMillis(),
                    0,
                    SessionStatus.ABANDONED
                )
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun ensureForeground() {
        postOngoingNotification()
    }

    private fun updateNotification() {
        postOngoingNotification()
    }

    /**
     * The foreground notification can only be replaced reliably through
     * startForeground(); NotificationManager.notify() leaves the old FGS
     * notification (and its actions) untouched on several Android versions.
     */
    private fun postOngoingNotification() {
        val state = TimerStore.snapshot()
        val taskTitle = state.taskId?.let { id ->
            runBlocking(Dispatchers.IO) {
                TomatoApp.instance.database.taskDao().getById(id)?.title
            }
        }
        val notification = NotificationHelper.ongoing(this, state, taskTitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIF_TIMER,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIF_TIMER, notification)
        }
        TimerWidgetProvider.updateAll(this)
    }

    private fun stopAndSelf() {
        tickJob?.cancel()
        AlarmScheduler.cancel(this)
        TimerWidgetProvider.updateAll(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun plannedFor(mode: TimerMode, settings: AppSettings): Int = when (mode) {
        TimerMode.FOCUS -> settings.workMinutes * 60
        TimerMode.SHORT_BREAK -> settings.shortBreakMinutes * 60
        TimerMode.LONG_BREAK -> settings.longBreakMinutes * 60
    }.coerceAtLeast(60)

    private fun readSettings(): AppSettings =
        runBlocking(Dispatchers.IO) { TomatoApp.instance.settings.settings.first() }

    private fun playAlert(settings: AppSettings) {
        if (settings.soundEnabled) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(this, uri)?.play()
            }
        }
        if (settings.vibrationEnabled) {
            runCatching {
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Vibrator::class.java)?.vibrate(effect)
                }
            }
        }
    }
}

private fun TimerMode.toSessionType(): String = when (this) {
    TimerMode.FOCUS -> SessionType.FOCUS
    TimerMode.SHORT_BREAK -> SessionType.SHORT_BREAK
    TimerMode.LONG_BREAK -> SessionType.LONG_BREAK
}
