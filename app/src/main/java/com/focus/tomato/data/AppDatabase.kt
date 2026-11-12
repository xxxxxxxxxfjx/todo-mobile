package com.focus.tomato.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY (completedAt IS NOT NULL), sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET completedPomodoros = completedPomodoros + 1 WHERE id = :id")
    suspend fun incrementCompletedPomodoros(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query(
        "UPDATE sessions SET endedAt = :endedAt, actualSeconds = :actualSeconds, status = :status WHERE id = :id"
    )
    suspend fun finish(id: Long, endedAt: Long, actualSeconds: Int, status: String)

    @Query(
        "SELECT * FROM sessions WHERE type = 'FOCUS' AND status = 'COMPLETED' AND dayKey >= :since ORDER BY startedAt DESC"
    )
    fun observeFocusSince(since: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'RUNNING' ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestRunning(): SessionEntity?

    @Query("DELETE FROM sessions")
    suspend fun clear()

    @Query("DELETE FROM sessions WHERE type = 'FOCUS'")
    suspend fun clearFocus()
}

@Database(
    entities = [TaskEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
}
