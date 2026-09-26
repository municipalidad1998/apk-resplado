package com.streamvault.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.streamvault.data.model.SavedSource;
import java.lang.Class;
import java.lang.Exception;
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
public final class SourcesDao_Impl implements SourcesDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SavedSource> __insertionAdapterOfSavedSource;

  private final EntityDeletionOrUpdateAdapter<SavedSource> __deletionAdapterOfSavedSource;

  public SourcesDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavedSource = new EntityInsertionAdapter<SavedSource>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `saved_sources` (`id`,`name`,`type`,`m3uUrl`,`localFilePath`,`xtreamUrl`,`xtreamUser`,`xtreamPass`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedSource entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getType());
        }
        if (entity.getM3uUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getM3uUrl());
        }
        if (entity.getLocalFilePath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getLocalFilePath());
        }
        if (entity.getXtreamUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getXtreamUrl());
        }
        if (entity.getXtreamUser() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getXtreamUser());
        }
        if (entity.getXtreamPass() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getXtreamPass());
        }
      }
    };
    this.__deletionAdapterOfSavedSource = new EntityDeletionOrUpdateAdapter<SavedSource>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `saved_sources` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedSource entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
      }
    };
  }

  @Override
  public Object insert(final SavedSource source, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSavedSource.insert(source);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final SavedSource source, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSavedSource.handle(source);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SavedSource>> getAll() {
    final String _sql = "SELECT * FROM saved_sources";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"saved_sources"}, new Callable<List<SavedSource>>() {
      @Override
      @NonNull
      public List<SavedSource> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfM3uUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "m3uUrl");
          final int _cursorIndexOfLocalFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "localFilePath");
          final int _cursorIndexOfXtreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "xtreamUrl");
          final int _cursorIndexOfXtreamUser = CursorUtil.getColumnIndexOrThrow(_cursor, "xtreamUser");
          final int _cursorIndexOfXtreamPass = CursorUtil.getColumnIndexOrThrow(_cursor, "xtreamPass");
          final List<SavedSource> _result = new ArrayList<SavedSource>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedSource _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpType;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmpType = null;
            } else {
              _tmpType = _cursor.getString(_cursorIndexOfType);
            }
            final String _tmpM3uUrl;
            if (_cursor.isNull(_cursorIndexOfM3uUrl)) {
              _tmpM3uUrl = null;
            } else {
              _tmpM3uUrl = _cursor.getString(_cursorIndexOfM3uUrl);
            }
            final String _tmpLocalFilePath;
            if (_cursor.isNull(_cursorIndexOfLocalFilePath)) {
              _tmpLocalFilePath = null;
            } else {
              _tmpLocalFilePath = _cursor.getString(_cursorIndexOfLocalFilePath);
            }
            final String _tmpXtreamUrl;
            if (_cursor.isNull(_cursorIndexOfXtreamUrl)) {
              _tmpXtreamUrl = null;
            } else {
              _tmpXtreamUrl = _cursor.getString(_cursorIndexOfXtreamUrl);
            }
            final String _tmpXtreamUser;
            if (_cursor.isNull(_cursorIndexOfXtreamUser)) {
              _tmpXtreamUser = null;
            } else {
              _tmpXtreamUser = _cursor.getString(_cursorIndexOfXtreamUser);
            }
            final String _tmpXtreamPass;
            if (_cursor.isNull(_cursorIndexOfXtreamPass)) {
              _tmpXtreamPass = null;
            } else {
              _tmpXtreamPass = _cursor.getString(_cursorIndexOfXtreamPass);
            }
            _item = new SavedSource(_tmpId,_tmpName,_tmpType,_tmpM3uUrl,_tmpLocalFilePath,_tmpXtreamUrl,_tmpXtreamUser,_tmpXtreamPass);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
