package com.focus.tomato.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SessionEntity> __insertionAdapterOfSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfFinish;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  private final SharedSQLiteStatement __preparedStmtOfClearFocus;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSessionEntity = new EntityInsertionAdapter<SessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sessions` (`id`,`taskId`,`type`,`plannedSeconds`,`actualSeconds`,`startedAt`,`endedAt`,`status`,`dayKey`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SessionEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTaskId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getTaskId());
        }
        statement.bindString(3, entity.getType());
        statement.bindLong(4, entity.getPlannedSeconds());
        statement.bindLong(5, entity.getActualSeconds());
        statement.bindLong(6, entity.getStartedAt());
        if (entity.getEndedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getEndedAt());
        }
        statement.bindString(8, entity.getStatus());
        statement.bindString(9, entity.getDayKey());
      }
    };
    this.__preparedStmtOfFinish = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sessions SET endedAt = ?, actualSeconds = ?, status = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sessions";
        return _query;
      }
    };
    this.__preparedStmtOfClearFocus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sessions WHERE type = 'FOCUS'";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SessionEntity session, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSessionEntity.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object finish(final long id, final long endedAt, final int actualSeconds,
      final String status, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfFinish.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, endedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, actualSeconds);
        _argIndex = 3;
        _stmt.bindString(_argIndex, status);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfFinish.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearFocus(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearFocus.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearFocus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SessionEntity>> observeFocusSince(final String since) {
    final String _sql = "SELECT * FROM sessions WHERE type = 'FOCUS' AND status = 'COMPLETED' AND dayKey >= ? ORDER BY startedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessions"}, new Callable<List<SessionEntity>>() {
      @Override
      @NonNull
      public List<SessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPlannedSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedSeconds");
          final int _cursorIndexOfActualSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "actualSeconds");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDayKey = CursorUtil.getColumnIndexOrThrow(_cursor, "dayKey");
          final List<SessionEntity> _result = new ArrayList<SessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getLong(_cursorIndexOfTaskId);
            }
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpPlannedSeconds;
            _tmpPlannedSeconds = _cursor.getInt(_cursorIndexOfPlannedSeconds);
            final int _tmpActualSeconds;
            _tmpActualSeconds = _cursor.getInt(_cursorIndexOfActualSeconds);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final Long _tmpEndedAt;
            if (_cursor.isNull(_cursorIndexOfEndedAt)) {
              _tmpEndedAt = null;
            } else {
              _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpDayKey;
            _tmpDayKey = _cursor.getString(_cursorIndexOfDayKey);
            _item = new SessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpPlannedSeconds,_tmpActualSeconds,_tmpStartedAt,_tmpEndedAt,_tmpStatus,_tmpDayKey);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object latestRunning(final Continuation<? super SessionEntity> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE status = 'RUNNING' ORDER BY startedAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SessionEntity>() {
      @Override
      @Nullable
      public SessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPlannedSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedSeconds");
          final int _cursorIndexOfActualSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "actualSeconds");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfDayKey = CursorUtil.getColumnIndexOrThrow(_cursor, "dayKey");
          final SessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getLong(_cursorIndexOfTaskId);
            }
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpPlannedSeconds;
            _tmpPlannedSeconds = _cursor.getInt(_cursorIndexOfPlannedSeconds);
            final int _tmpActualSeconds;
            _tmpActualSeconds = _cursor.getInt(_cursorIndexOfActualSeconds);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final Long _tmpEndedAt;
            if (_cursor.isNull(_cursorIndexOfEndedAt)) {
              _tmpEndedAt = null;
            } else {
              _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpDayKey;
            _tmpDayKey = _cursor.getString(_cursorIndexOfDayKey);
            _result = new SessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpPlannedSeconds,_tmpActualSeconds,_tmpStartedAt,_tmpEndedAt,_tmpStatus,_tmpDayKey);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
