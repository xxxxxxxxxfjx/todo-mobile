package com.focus.tomato.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focus.tomato.TomatoApp
import com.focus.tomato.data.AppSettings
import com.focus.tomato.data.SessionEntity
import com.focus.tomato.data.TaskEntity
import com.focus.tomato.data.TimerMode
import com.focus.tomato.data.TimerStatus
import com.focus.tomato.data.TimerStore
import com.focus.tomato.timer.FocusTimerService
import com.focus.tomato.timer.TimerActions
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayPoint(val date: LocalDate, val seconds: Long, val count: Int)

data class Achievement(
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

data class StatsUi(
    val todaySeconds: Long = 0,
    val todayCount: Int = 0,
    val weekSeconds: Long = 0,
    val weekCount: Int = 0,
    val totalSeconds: Long = 0,
    val totalCount: Int = 0,
    val streak: Int = 0,
    val last7: List<DayPoint> = emptyList(),
    val heatmap: List<DayPoint> = emptyList(),
    val distribution: List<Pair<String, Long>> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TomatoApp.instance.database
    private val settingsRepo = TomatoApp.instance.settings
    private val app = application

    val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    val tasks: StateFlow<List<TaskEntity>> = db.taskDao().observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val sessions: StateFlow<List<SessionEntity>> = db.sessionDao()
        .observeFocusSince(LocalDate.now().minusDays(365).toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val timerState: StateFlow<com.focus.tomato.data.TimerState> = TimerStore.state

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    val selectedTaskId: StateFlow<Long?> = _selectedTaskId.asStateFlow()

    val selectedTask: StateFlow<TaskEntity?> = combine(tasks, selectedTaskId) { list, id ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stats: StateFlow<StatsUi> = combine(sessions, tasks) { sessionList, taskList ->
        buildStats(sessionList, taskList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildStats(emptyList(), emptyList()))

    // ------------------------------------------------------------------ tasks

    fun addTask(title: String, note: String, estimated: Int, priority: Int) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().insert(
                TaskEntity(
                    title = clean,
                    note = note.trim(),
                    estimatedPomodoros = estimated.coerceIn(1, 20),
                    priority = priority.coerceIn(0, 3)
                )
            )
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.taskDao().update(task) }
    }

    fun toggleTask(task: TaskEntity) {
        val completed = task.completedAt == null
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().update(
                task.copy(completedAt = if (completed) System.currentTimeMillis() else null)
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.taskDao().delete(task) }
        if (_selectedTaskId.value == task.id) _selectedTaskId.value = null
    }

    fun selectTask(taskId: Long?) {
        _selectedTaskId.value = taskId
    }

    // ------------------------------------------------------------------ timer

    fun startFocus(taskId: Long? = selectedTaskId.value) {
        val s = settings.value
        _selectedTaskId.value = taskId
        FocusTimerService.start(app, TimerMode.FOCUS, taskId, s.workMinutes * 60)
    }

    fun startCurrentMode() {
        val state = TimerStore.state.value
        val settingsNow = settings.value
        val seconds = when (state.mode) {
            TimerMode.FOCUS -> settingsNow.workMinutes * 60
            TimerMode.SHORT_BREAK -> settingsNow.shortBreakMinutes * 60
            TimerMode.LONG_BREAK -> settingsNow.longBreakMinutes * 60
        }
        FocusTimerService.start(
            app,
            state.mode,
            if (state.mode == TimerMode.FOCUS) selectedTaskId.value else null,
            seconds
        )
    }

    fun pause() = FocusTimerService.command(app, TimerActions.ACTION_PAUSE)
    fun resume() = FocusTimerService.command(app, TimerActions.ACTION_RESUME)
    fun reset() = FocusTimerService.command(app, TimerActions.ACTION_RESET)
    fun skip() = FocusTimerService.command(app, TimerActions.ACTION_SKIP)
    fun extend() = FocusTimerService.command(app, TimerActions.ACTION_EXTEND)

    /** Debug-only short session used to verify completion, alerts and statistics. */
    fun startDebugSession(seconds: Int = 60) {
        FocusTimerService.start(app, TimerMode.FOCUS, selectedTaskId.value, seconds)
    }

    fun switchMode(mode: TimerMode) {
        val state = TimerStore.state.value
        if (state.status == TimerStatus.RUNNING) return
        val s = settings.value
        val seconds = when (mode) {
            TimerMode.FOCUS -> s.workMinutes * 60
            TimerMode.SHORT_BREAK -> s.shortBreakMinutes * 60
            TimerMode.LONG_BREAK -> s.longBreakMinutes * 60
        }
        TimerStore.setMode(mode, seconds)
    }

    fun onAppForeground() {
        if (TimerStore.state.value.status == TimerStatus.RUNNING ||
            TimerStore.state.value.status == TimerStatus.PAUSED
        ) {
            FocusTimerService.reconcile(app)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    fun clearFocusRecords() {
        FocusTimerService.command(app, TimerActions.ACTION_RESET)
        viewModelScope.launch(Dispatchers.IO) {
            db.sessionDao().clearFocus()
        }
    }

    fun clearAllData() {
        FocusTimerService.command(app, TimerActions.ACTION_RESET)
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().clear()
            db.sessionDao().clear()
        }
        _selectedTaskId.value = null
        TimerStore.stopToIdle(TimerMode.FOCUS, settings.value.workMinutes * 60, 0)
    }

    // ------------------------------------------------------------------ stats

    private fun buildStats(sessions: List<SessionEntity>, tasks: List<TaskEntity>): StatsUi {
        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val byDay = sessions.groupBy { it.dayKey }
        fun secondsOf(date: LocalDate): Long = byDay[date.toString()]?.sumOf { it.actualSeconds.toLong() } ?: 0L
        fun countOf(date: LocalDate): Int = byDay[date.toString()]?.size ?: 0

        val todaySeconds = secondsOf(today)
        val todayCount = countOf(today)
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }.filter { !it.isAfter(today) }
        val weekSeconds = weekDates.sumOf { secondsOf(it) }
        val weekCount = weekDates.sumOf { countOf(it) }
        val totalSeconds = sessions.sumOf { it.actualSeconds.toLong() }
        val totalCount = sessions.size

        val activeDays = sessions.mapNotNull { runCatching { LocalDate.parse(it.dayKey) }.getOrNull() }.toSet()
        var cursor = if (activeDays.contains(today)) today else today.minusDays(1)
        var streak = 0
        while (activeDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val last7 = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayPoint(date, secondsOf(date), countOf(date))
        }
        val heatStart = today.minusWeeks(11).with(DayOfWeek.MONDAY)
        val heatmap = (0 until 84).map { offset ->
            val date = heatStart.plusDays(offset.toLong())
            DayPoint(date, secondsOf(date), countOf(date))
        }

        val taskTitles = tasks.associate { it.id to it.title }
        val distribution = sessions.groupBy { it.taskId }
            .map { (taskId, rows) ->
                (taskId?.let { taskTitles[it] } ?: "未关联任务") to rows.sumOf { it.actualSeconds.toLong() }
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)

        val completedTasks = tasks.count { it.completedAt != null }
        val achievements = listOf(
            Achievement("🍅", "第一颗番茄", "完成第一个专注", totalCount >= 1),
            Achievement("🔥", "连续三天", "连续 3 天保持专注", streak >= 3),
            Achievement("⏳", "十小时", "累计专注 10 小时", totalSeconds >= 10 * 3600),
            Achievement("✅", "任务收割机", "完成 10 个任务", completedTasks >= 10)
        )

        return StatsUi(
            todaySeconds = todaySeconds,
            todayCount = todayCount,
            weekSeconds = weekSeconds,
            weekCount = weekCount,
            totalSeconds = totalSeconds,
            totalCount = totalCount,
            streak = streak,
            last7 = last7,
            heatmap = heatmap,
            distribution = distribution,
            achievements = achievements
        )
    }
}
