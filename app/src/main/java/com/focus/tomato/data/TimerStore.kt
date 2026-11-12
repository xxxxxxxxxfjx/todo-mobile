package com.focus.tomato.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerMode(val label: String, val shortLabel: String, val emoji: String) {
    FOCUS("专注", "专注", "🍅"),
    SHORT_BREAK("短休息", "短休", "☕"),
    LONG_BREAK("长休息", "长休", "🌿")
}

enum class TimerStatus { IDLE, RUNNING, PAUSED }

data class TimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val plannedSeconds: Int = 25 * 60,
    val remainingMs: Long = 25 * 60 * 1000L,
    /** elapsedRealtime() deadline, valid within one boot */
    val endAtElapsed: Long = 0L,
    /** wall-clock deadline, used after a reboot */
    val endAtWall: Long = 0L,
    val taskId: Long? = null,
    val sessionId: Long? = null,
    val sessionStartedAt: Long = 0L,
    /** Focus sessions completed since the last long break. */
    val focusInCycle: Int = 0
)

/**
 * Single in-process source of truth for the running timer. The state is persisted
 * synchronously at every transition, while per-tick remaining updates stay in memory.
 */
object TimerStore {
    private const val PREFS = "timer_state_v1"
    private const val KEY_MODE = "mode"
    private const val KEY_STATUS = "status"
    private const val KEY_PLANNED = "planned_seconds"
    private const val KEY_REMAINING = "remaining_ms"
    private const val KEY_END_ELAPSED = "end_at_elapsed"
    private const val KEY_END_WALL = "end_at_wall"
    private const val KEY_TASK_ID = "task_id"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SESSION_STARTED = "session_started_at"
    private const val KEY_CYCLE = "focus_in_cycle"
    private const val KEY_BOOT_COUNT = "boot_count"

    private const val MAX_TIMER_MS = 12L * 60 * 60 * 1000

    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null
    private var initialized = false

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = load()
        initialized = true
        // Record the current boot generation so the next launch can tell a
        // reboot (elapsedRealtime resets) from an ordinary process restart.
        persist()
    }

    private fun bootCount(): Int {
        val fromSettings = runCatching {
            val ctx = appContext ?: return@runCatching -1
            Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, -1)
        }.getOrDefault(-1)
        if (fromSettings >= 0) return fromSettings
        return runCatching {
            java.io.File("/proc/sys/kernel/random/boot_id").readText().trim().hashCode()
        }.getOrDefault(-1)
    }

    private fun load(): TimerState {
        val mode = runCatching { TimerMode.valueOf(prefs.getString(KEY_MODE, TimerMode.FOCUS.name)!!) }
            .getOrDefault(TimerMode.FOCUS)
        val status = runCatching { TimerStatus.valueOf(prefs.getString(KEY_STATUS, TimerStatus.IDLE.name)!!) }
            .getOrDefault(TimerStatus.IDLE)
        val planned = prefs.getInt(KEY_PLANNED, mode.defaultSeconds())
        val savedRemaining = prefs.getLong(KEY_REMAINING, planned * 1000L)
        val endAtElapsed = prefs.getLong(KEY_END_ELAPSED, 0L)
        val endAtWall = prefs.getLong(KEY_END_WALL, 0L)
        val savedBoot = prefs.getInt(KEY_BOOT_COUNT, -1)
        val currentBoot = bootCount()
        val rebooted = status == TimerStatus.RUNNING &&
            savedBoot >= 0 && currentBoot >= 0 && savedBoot != currentBoot
        val remaining = if (rebooted) {
            (endAtWall - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            savedRemaining
        }
        val rebasedElapsed = if (rebooted) SystemClock.elapsedRealtime() + remaining else endAtElapsed
        val rebasedWall = if (rebooted) System.currentTimeMillis() + remaining else endAtWall
        return TimerState(
            mode = mode,
            status = status,
            plannedSeconds = planned,
            remainingMs = remaining,
            endAtElapsed = rebasedElapsed,
            endAtWall = rebasedWall,
            taskId = prefs.getLong(KEY_TASK_ID, -1L).takeIf { it > 0 },
            sessionId = prefs.getLong(KEY_SESSION_ID, -1L).takeIf { it > 0 },
            sessionStartedAt = prefs.getLong(KEY_SESSION_STARTED, 0L),
            focusInCycle = prefs.getInt(KEY_CYCLE, 0)
        )
    }

    fun computeRemaining(s: TimerState = _state.value): Long {
        if (s.status != TimerStatus.RUNNING) return s.remainingMs.coerceAtLeast(0L)
        val byElapsed = s.endAtElapsed - SystemClock.elapsedRealtime()
        val byWall = s.endAtWall - System.currentTimeMillis()
        return if (byElapsed > MAX_TIMER_MS || byElapsed < -MAX_TIMER_MS) {
            byWall.coerceIn(0L, MAX_TIMER_MS)
        } else {
            byElapsed.coerceAtLeast(0L)
        }
    }

    fun snapshot(): TimerState = _state.value.copy(remainingMs = computeRemaining())

    fun setRemaining(remainingMs: Long) {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING) return
        _state.value = s.copy(remainingMs = remainingMs.coerceAtLeast(0L))
    }

    fun setMode(mode: TimerMode, plannedSeconds: Int) {
        val s = _state.value
        if (s.status == TimerStatus.RUNNING) return
        _state.value = s.copy(
            mode = mode,
            status = TimerStatus.IDLE,
            plannedSeconds = plannedSeconds,
            remainingMs = plannedSeconds * 1000L,
            endAtElapsed = 0L,
            endAtWall = 0L
        )
        persist()
    }

    fun markRunning(
        mode: TimerMode,
        plannedSeconds: Int,
        taskId: Long?,
        sessionId: Long?,
        sessionStartedAt: Long,
        focusInCycle: Int
    ) {
        val now = SystemClock.elapsedRealtime()
        _state.value = TimerState(
            mode = mode,
            status = TimerStatus.RUNNING,
            plannedSeconds = plannedSeconds,
            remainingMs = plannedSeconds * 1000L,
            endAtElapsed = now + plannedSeconds * 1000L,
            endAtWall = System.currentTimeMillis() + plannedSeconds * 1000L,
            taskId = taskId,
            sessionId = sessionId,
            sessionStartedAt = sessionStartedAt,
            focusInCycle = focusInCycle
        )
        persist()
    }

    fun pause() {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING) return
        _state.value = s.copy(
            status = TimerStatus.PAUSED,
            remainingMs = computeRemaining(s),
            endAtElapsed = 0L,
            endAtWall = 0L
        )
        persist()
    }

    fun resume() {
        val s = _state.value
        if (s.status != TimerStatus.PAUSED) return
        val remaining = s.remainingMs.coerceAtLeast(0L)
        val now = SystemClock.elapsedRealtime()
        _state.value = s.copy(
            status = TimerStatus.RUNNING,
            endAtElapsed = now + remaining,
            endAtWall = System.currentTimeMillis() + remaining
        )
        persist()
    }

    fun extend(seconds: Int) {
        val s = _state.value
        if (s.status == TimerStatus.IDLE) return
        val extra = seconds * 1000L
        _state.value = s.copy(
            plannedSeconds = s.plannedSeconds + seconds,
            remainingMs = s.remainingMs + extra,
            endAtElapsed = if (s.status == TimerStatus.RUNNING) s.endAtElapsed + extra else 0L,
            endAtWall = if (s.status == TimerStatus.RUNNING) s.endAtWall + extra else 0L
        )
        persist()
    }

    fun stopToIdle(mode: TimerMode, plannedSeconds: Int, focusInCycle: Int) {
        _state.value = TimerState(
            mode = mode,
            status = TimerStatus.IDLE,
            plannedSeconds = plannedSeconds,
            remainingMs = plannedSeconds * 1000L,
            focusInCycle = focusInCycle
        )
        persist()
    }

    /** Re-anchor a running timer after a reboot or a long sleep. */
    fun rebase(remainingMs: Long) {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING) return
        val now = SystemClock.elapsedRealtime()
        _state.value = s.copy(
            remainingMs = remainingMs.coerceAtLeast(0L),
            endAtElapsed = now + remainingMs.coerceAtLeast(0L),
            endAtWall = System.currentTimeMillis() + remainingMs.coerceAtLeast(0L)
        )
        persist()
    }

    fun updateSessionId(sessionId: Long?) {
        _state.value = _state.value.copy(sessionId = sessionId)
        persist()
    }

    fun clearPersistedForTests() {
        prefs.edit().clear().commit()
        _state.value = TimerState()
    }

    private fun persist() {
        val s = _state.value
        prefs.edit()
            .putString(KEY_MODE, s.mode.name)
            .putString(KEY_STATUS, s.status.name)
            .putInt(KEY_PLANNED, s.plannedSeconds)
            .putLong(KEY_REMAINING, s.remainingMs)
            .putLong(KEY_END_ELAPSED, s.endAtElapsed)
            .putLong(KEY_END_WALL, s.endAtWall)
            .putLong(KEY_TASK_ID, s.taskId ?: -1L)
            .putLong(KEY_SESSION_ID, s.sessionId ?: -1L)
            .putLong(KEY_SESSION_STARTED, s.sessionStartedAt)
            .putInt(KEY_CYCLE, s.focusInCycle)
            .putInt(KEY_BOOT_COUNT, bootCount())
            .commit()
    }
}

fun TimerMode.defaultSeconds(): Int = when (this) {
    TimerMode.FOCUS -> 25 * 60
    TimerMode.SHORT_BREAK -> 5 * 60
    TimerMode.LONG_BREAK -> 15 * 60
}
