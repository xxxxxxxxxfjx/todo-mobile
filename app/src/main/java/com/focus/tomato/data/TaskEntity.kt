package com.focus.tomato.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A task the user wants to spend pomodoros on. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    /** 0 = none, 1 = low, 2 = medium, 3 = high */
    val priority: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

/** One recorded timer phase. Only FOCUS + COMPLETED rows count towards statistics. */
@Entity(
    tableName = "sessions",
    indices = [Index("dayKey"), Index("startedAt")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    /** FOCUS, SHORT_BREAK, LONG_BREAK */
    val type: String = SessionType.FOCUS,
    val plannedSeconds: Int,
    val actualSeconds: Int = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    /** RUNNING, COMPLETED, ABANDONED */
    val status: String = SessionStatus.RUNNING,
    val dayKey: String
)

object SessionType {
    const val FOCUS = "FOCUS"
    const val SHORT_BREAK = "SHORT_BREAK"
    const val LONG_BREAK = "LONG_BREAK"
}

object SessionStatus {
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val ABANDONED = "ABANDONED"
}
