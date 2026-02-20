package com.moneytracker.app.data.local.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.moneytracker.app.data.local.database.converters.Converters;
import com.moneytracker.app.data.local.database.entities.CategorySpending;
import com.moneytracker.app.data.local.database.entities.RecurringUnit;
import com.moneytracker.app.data.local.database.entities.SyncStatus;
import com.moneytracker.app.data.local.database.entities.TransactionEntity;
import com.moneytracker.app.data.local.database.entities.TransactionSplitEntity;
import com.moneytracker.app.data.local.database.entities.TransactionType;
import com.moneytracker.app.data.local.database.entities.TransactionWithSplits;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransactionEntity> __insertionAdapterOfTransactionEntity;

  private final Converters __converters = new Converters();

  private final EntityInsertionAdapter<TransactionSplitEntity> __insertionAdapterOfTransactionSplitEntity;

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __updateAdapterOfTransactionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSplitsByTransactionId;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteTransaction;

  private final SharedSQLiteStatement __preparedStmtOfMarkAsSynced;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllByAccount;

  private final SharedSQLiteStatement __preparedStmtOfClearAllTransactions;

  private final SharedSQLiteStatement __preparedStmtOfClearAllSplits;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransactionEntity = new EntityInsertionAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`accountId`,`payee`,`note`,`date`,`totalAmount`,`type`,`toAccountId`,`createdAt`,`modifiedAt`,`isDeleted`,`syncStatus`,`isRecurring`,`recurringInterval`,`recurringUnit`,`recurringEndDate`,`parentRecurringId`,`notifyForRecurringEntries`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getPayee());
        if (entity.getNote() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getNote());
        }
        statement.bindLong(5, entity.getDate());
        statement.bindDouble(6, entity.getTotalAmount());
        final String _tmp = __converters.fromTransactionType(entity.getType());
        statement.bindString(7, _tmp);
        if (entity.getToAccountId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getToAccountId());
        }
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getModifiedAt());
        final int _tmp_1 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final String _tmp_2 = __converters.fromSyncStatus(entity.getSyncStatus());
        statement.bindString(12, _tmp_2);
        final int _tmp_3 = entity.isRecurring() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
        if (entity.getRecurringInterval() == null) {
          statement.bindNull(14);
        } else {
          statement.bindLong(14, entity.getRecurringInterval());
        }
        if (entity.getRecurringUnit() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, __RecurringUnit_enumToString(entity.getRecurringUnit()));
        }
        if (entity.getRecurringEndDate() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getRecurringEndDate());
        }
        if (entity.getParentRecurringId() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getParentRecurringId());
        }
        final int _tmp_4 = entity.getNotifyForRecurringEntries() ? 1 : 0;
        statement.bindLong(18, _tmp_4);
      }
    };
    this.__insertionAdapterOfTransactionSplitEntity = new EntityInsertionAdapter<TransactionSplitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transaction_splits` (`id`,`transactionId`,`categoryId`,`amount`,`note`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionSplitEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTransactionId());
        statement.bindString(3, entity.getCategoryId());
        statement.bindDouble(4, entity.getAmount());
        if (entity.getNote() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getNote());
        }
      }
    };
    this.__updateAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`accountId` = ?,`payee` = ?,`note` = ?,`date` = ?,`totalAmount` = ?,`type` = ?,`toAccountId` = ?,`createdAt` = ?,`modifiedAt` = ?,`isDeleted` = ?,`syncStatus` = ?,`isRecurring` = ?,`recurringInterval` = ?,`recurringUnit` = ?,`recurringEndDate` = ?,`parentRecurringId` = ?,`notifyForRecurringEntries` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindString(3, entity.getPayee());
        if (entity.getNote() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getNote());
        }
        statement.bindLong(5, entity.getDate());
        statement.bindDouble(6, entity.getTotalAmount());
        final String _tmp = __converters.fromTransactionType(entity.getType());
        statement.bindString(7, _tmp);
        if (entity.getToAccountId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getToAccountId());
        }
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getModifiedAt());
        final int _tmp_1 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final String _tmp_2 = __converters.fromSyncStatus(entity.getSyncStatus());
        statement.bindString(12, _tmp_2);
        final int _tmp_3 = entity.isRecurring() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
        if (entity.getRecurringInterval() == null) {
          statement.bindNull(14);
        } else {
          statement.bindLong(14, entity.getRecurringInterval());
        }
        if (entity.getRecurringUnit() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, __RecurringUnit_enumToString(entity.getRecurringUnit()));
        }
        if (entity.getRecurringEndDate() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getRecurringEndDate());
        }
        if (entity.getParentRecurringId() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getParentRecurringId());
        }
        final int _tmp_4 = entity.getNotifyForRecurringEntries() ? 1 : 0;
        statement.bindLong(18, _tmp_4);
        statement.bindString(19, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteSplitsByTransactionId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transaction_splits WHERE transactionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSoftDeleteTransaction = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE transactions SET isDeleted = 1, modifiedAt = ?, syncStatus = 'DIRTY' WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAsSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE transactions SET syncStatus = 'SYNCED' WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllByAccount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE accountId = ? OR toAccountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllTransactions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllSplits = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transaction_splits";
        return _query;
      }
    };
  }

  @Override
  public Object insertTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionEntity.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSplits(final List<TransactionSplitEntity> splits,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionSplitEntity.insert(splits);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveFullTransaction(final TransactionEntity transaction,
      final List<TransactionSplitEntity> splits, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> TransactionDao.DefaultImpls.saveFullTransaction(TransactionDao_Impl.this, transaction, splits, __cont), $completion);
  }

  @Override
  public Object updateFullTransaction(final TransactionEntity transaction,
      final List<TransactionSplitEntity> splits, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> TransactionDao.DefaultImpls.updateFullTransaction(TransactionDao_Impl.this, transaction, splits, __cont), $completion);
  }

  @Override
  public Object deleteSplitsByTransactionId(final String transactionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSplitsByTransactionId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, transactionId);
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
          __preparedStmtOfDeleteSplitsByTransactionId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteTransaction(final String id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteTransaction.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfSoftDeleteTransaction.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsSynced(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfMarkAsSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllByAccount(final String accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllByAccount.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, accountId);
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
          __preparedStmtOfDeleteAllByAccount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllTransactions(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllTransactions.acquire();
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
          __preparedStmtOfClearAllTransactions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllSplits(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllSplits.acquire();
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
          __preparedStmtOfClearAllSplits.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TransactionWithSplits>> getAllTransactionsWithDetails() {
    final String _sql = "\n"
            + "        SELECT * FROM transactions \n"
            + "        WHERE isDeleted = 0 \n"
            + "        ORDER BY date DESC, createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionWithSplits>> getTransactionsByDateRange(final long startDate,
      final long endDate) {
    final String _sql = "\n"
            + "        SELECT * FROM transactions \n"
            + "        WHERE date BETWEEN ? AND ? \n"
            + "        AND isDeleted = 0 \n"
            + "        ORDER BY date DESC, createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionWithSplits>> getTransactionsByAccount(final String accountId) {
    final String _sql = "\n"
            + "        SELECT * FROM transactions \n"
            + "        WHERE accountId = ? \n"
            + "        AND isDeleted = 0 \n"
            + "        ORDER BY date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionWithSplits>> getTransactionsByCategory(final String categoryId,
      final String type) {
    final String _sql = "\n"
            + "        SELECT DISTINCT t.* FROM transactions t\n"
            + "        JOIN transaction_splits s ON s.transactionId = t.id\n"
            + "        WHERE s.categoryId = ?\n"
            + "        AND t.type = ?\n"
            + "        AND t.isDeleted = 0\n"
            + "        ORDER BY t.date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    _argIndex = 2;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionWithSplits>> getTransactionsByCategoryForPeriod(
      final String categoryId, final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT DISTINCT t.* FROM transactions t\n"
            + "        JOIN transaction_splits s ON s.transactionId = t.id\n"
            + "        WHERE s.categoryId = ?\n"
            + "        AND t.type = 'EXPENSE'\n"
            + "        AND t.isDeleted = 0\n"
            + "        AND t.date BETWEEN ? AND ?\n"
            + "        ORDER BY t.date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TransactionWithSplits>> getTransactionsByAccountIncludingTransfers(
      final String accountId) {
    final String _sql = "\n"
            + "        SELECT * FROM transactions \n"
            + "        WHERE (accountId = ? OR toAccountId = ?)\n"
            + "        AND isDeleted = 0 \n"
            + "        ORDER BY date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTransactionById(final String id,
      final Continuation<? super TransactionWithSplits> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE id = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<TransactionWithSplits>() {
      @Override
      @Nullable
      public TransactionWithSplits call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final TransactionWithSplits _result;
            if (_cursor.moveToFirst()) {
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _result = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTransactionByIdInternal(final String id,
      final Continuation<? super TransactionWithSplits> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE id = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<TransactionWithSplits>() {
      @Override
      @Nullable
      public TransactionWithSplits call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final TransactionWithSplits _result;
            if (_cursor.moveToFirst()) {
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _result = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Double> getTotalExpenseForPeriod(final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT SUM(t.totalAmount)\n"
            + "        FROM transactions t\n"
            + "        WHERE t.type = 'EXPENSE'\n"
            + "        AND t.date BETWEEN ? AND ?\n"
            + "        AND t.isDeleted = 0\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
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
  public Flow<Double> getTotalIncomeForPeriod(final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT SUM(t.totalAmount)\n"
            + "        FROM transactions t\n"
            + "        WHERE t.type = 'INCOME'\n"
            + "        AND t.date BETWEEN ? AND ?\n"
            + "        AND t.isDeleted = 0\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
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
  public Flow<Double> getTotalTransferForPeriod(final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT SUM(t.totalAmount)\n"
            + "        FROM transactions t\n"
            + "        WHERE t.type = 'TRANSFER'\n"
            + "        AND t.date BETWEEN ? AND ?\n"
            + "        AND t.isDeleted = 0\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
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
  public Flow<List<CategorySpending>> getCategorySpendingForPeriod(final long startDate,
      final long endDate) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            c.id as categoryId,\n"
            + "            c.name as categoryName, \n"
            + "            c.colorHex as colorHex,\n"
            + "            c.iconKey as iconKey,\n"
            + "            SUM(s.amount) as total \n"
            + "        FROM transaction_splits s\n"
            + "        JOIN categories c ON s.categoryId = c.id\n"
            + "        JOIN transactions t ON s.transactionId = t.id\n"
            + "        WHERE t.date BETWEEN ? AND ?\n"
            + "        AND t.type = 'EXPENSE'\n"
            + "        AND t.isDeleted = 0\n"
            + "        GROUP BY c.id\n"
            + "        ORDER BY total DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transaction_splits", "categories",
        "transactions"}, new Callable<List<CategorySpending>>() {
      @Override
      @NonNull
      public List<CategorySpending> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCategoryId = 0;
          final int _cursorIndexOfCategoryName = 1;
          final int _cursorIndexOfColorHex = 2;
          final int _cursorIndexOfIconKey = 3;
          final int _cursorIndexOfTotal = 4;
          final List<CategorySpending> _result = new ArrayList<CategorySpending>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategorySpending _item;
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpCategoryName;
            _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final String _tmpIconKey;
            _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            _item = new CategorySpending(_tmpCategoryId,_tmpCategoryName,_tmpColorHex,_tmpIconKey,_tmpTotal);
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
  public Flow<List<CategorySpending>> getCategoryIncomeForPeriod(final long startDate,
      final long endDate) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            c.id as categoryId,\n"
            + "            c.name as categoryName, \n"
            + "            c.colorHex as colorHex,\n"
            + "            c.iconKey as iconKey,\n"
            + "            SUM(s.amount) as total \n"
            + "        FROM transaction_splits s\n"
            + "        JOIN categories c ON s.categoryId = c.id\n"
            + "        JOIN transactions t ON s.transactionId = t.id\n"
            + "        WHERE t.date BETWEEN ? AND ?\n"
            + "        AND t.type = 'INCOME'\n"
            + "        AND t.isDeleted = 0\n"
            + "        GROUP BY c.id\n"
            + "        ORDER BY total DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transaction_splits", "categories",
        "transactions"}, new Callable<List<CategorySpending>>() {
      @Override
      @NonNull
      public List<CategorySpending> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCategoryId = 0;
          final int _cursorIndexOfCategoryName = 1;
          final int _cursorIndexOfColorHex = 2;
          final int _cursorIndexOfIconKey = 3;
          final int _cursorIndexOfTotal = 4;
          final List<CategorySpending> _result = new ArrayList<CategorySpending>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CategorySpending _item;
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpCategoryName;
            _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final String _tmpIconKey;
            _tmpIconKey = _cursor.getString(_cursorIndexOfIconKey);
            final double _tmpTotal;
            _tmpTotal = _cursor.getDouble(_cursorIndexOfTotal);
            _item = new CategorySpending(_tmpCategoryId,_tmpCategoryName,_tmpColorHex,_tmpIconKey,_tmpTotal);
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
  public Flow<List<TransactionWithSplits>> getTransferTransactionsForPeriod(final long startDate,
      final long endDate) {
    final String _sql = "\n"
            + "        SELECT * FROM transactions\n"
            + "        WHERE type = 'TRANSFER'\n"
            + "        AND date BETWEEN ? AND ?\n"
            + "        AND isDeleted = 0\n"
            + "        ORDER BY date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"transaction_splits",
        "transactions"}, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllTransactionsOnce(
      final Continuation<? super List<TransactionWithSplits>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllTransactionsIncludingDeletedOnce(
      final Continuation<? super List<TransactionWithSplits>> $completion) {
    final String _sql = "SELECT * FROM transactions ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<List<TransactionWithSplits>>() {
      @Override
      @NonNull
      public List<TransactionWithSplits> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
            final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
            final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
            final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
            final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
            final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
            final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
            final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
            final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
            final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
            final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
            final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
            final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
            final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
            final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
            final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
            final ArrayMap<String, ArrayList<TransactionSplitEntity>> _collectionSplits = new ArrayMap<String, ArrayList<TransactionSplitEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionSplits.containsKey(_tmpKey)) {
                _collectionSplits.put(_tmpKey, new ArrayList<TransactionSplitEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(_collectionSplits);
            final List<TransactionWithSplits> _result = new ArrayList<TransactionWithSplits>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final TransactionWithSplits _item;
              final TransactionEntity _tmpTransaction;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpAccountId;
              _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
              final String _tmpPayee;
              _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
              final String _tmpNote;
              if (_cursor.isNull(_cursorIndexOfNote)) {
                _tmpNote = null;
              } else {
                _tmpNote = _cursor.getString(_cursorIndexOfNote);
              }
              final long _tmpDate;
              _tmpDate = _cursor.getLong(_cursorIndexOfDate);
              final double _tmpTotalAmount;
              _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
              final TransactionType _tmpType;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfType);
              _tmpType = __converters.toTransactionType(_tmp);
              final String _tmpToAccountId;
              if (_cursor.isNull(_cursorIndexOfToAccountId)) {
                _tmpToAccountId = null;
              } else {
                _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
              }
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpModifiedAt;
              _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
              final boolean _tmpIsDeleted;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
              _tmpIsDeleted = _tmp_1 != 0;
              final SyncStatus _tmpSyncStatus;
              final String _tmp_2;
              _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
              _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
              final boolean _tmpIsRecurring;
              final int _tmp_3;
              _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
              _tmpIsRecurring = _tmp_3 != 0;
              final Integer _tmpRecurringInterval;
              if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
                _tmpRecurringInterval = null;
              } else {
                _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
              }
              final RecurringUnit _tmpRecurringUnit;
              if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
                _tmpRecurringUnit = null;
              } else {
                _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
              }
              final Long _tmpRecurringEndDate;
              if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
                _tmpRecurringEndDate = null;
              } else {
                _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
              }
              final String _tmpParentRecurringId;
              if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
                _tmpParentRecurringId = null;
              } else {
                _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
              }
              final boolean _tmpNotifyForRecurringEntries;
              final int _tmp_4;
              _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
              _tmpNotifyForRecurringEntries = _tmp_4 != 0;
              _tmpTransaction = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
              final ArrayList<TransactionSplitEntity> _tmpSplitsCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpSplitsCollection = _collectionSplits.get(_tmpKey_1);
              _item = new TransactionWithSplits(_tmpTransaction,_tmpSplitsCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getUnsyncedTransactions(
      final Continuation<? super List<TransactionEntity>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE syncStatus != 'SYNCED' AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
          final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
          final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
          final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
          final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpPayee;
            _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toTransactionType(_tmp);
            final String _tmpToAccountId;
            if (_cursor.isNull(_cursorIndexOfToAccountId)) {
              _tmpToAccountId = null;
            } else {
              _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpModifiedAt;
            _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final SyncStatus _tmpSyncStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
            _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
            final boolean _tmpIsRecurring;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp_3 != 0;
            final Integer _tmpRecurringInterval;
            if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
              _tmpRecurringInterval = null;
            } else {
              _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
            }
            final RecurringUnit _tmpRecurringUnit;
            if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
              _tmpRecurringUnit = null;
            } else {
              _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
            }
            final Long _tmpRecurringEndDate;
            if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
              _tmpRecurringEndDate = null;
            } else {
              _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
            }
            final String _tmpParentRecurringId;
            if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
              _tmpParentRecurringId = null;
            } else {
              _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
            }
            final boolean _tmpNotifyForRecurringEntries;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
            _tmpNotifyForRecurringEntries = _tmp_4 != 0;
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTransfersFromAccount(final String accountId,
      final Continuation<? super List<TransactionEntity>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE type = 'TRANSFER' AND accountId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
          final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
          final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
          final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
          final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpPayee;
            _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toTransactionType(_tmp);
            final String _tmpToAccountId;
            if (_cursor.isNull(_cursorIndexOfToAccountId)) {
              _tmpToAccountId = null;
            } else {
              _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpModifiedAt;
            _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final SyncStatus _tmpSyncStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
            _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
            final boolean _tmpIsRecurring;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp_3 != 0;
            final Integer _tmpRecurringInterval;
            if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
              _tmpRecurringInterval = null;
            } else {
              _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
            }
            final RecurringUnit _tmpRecurringUnit;
            if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
              _tmpRecurringUnit = null;
            } else {
              _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
            }
            final Long _tmpRecurringEndDate;
            if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
              _tmpRecurringEndDate = null;
            } else {
              _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
            }
            final String _tmpParentRecurringId;
            if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
              _tmpParentRecurringId = null;
            } else {
              _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
            }
            final boolean _tmpNotifyForRecurringEntries;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
            _tmpNotifyForRecurringEntries = _tmp_4 != 0;
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTransfersToAccount(final String accountId,
      final Continuation<? super List<TransactionEntity>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE type = 'TRANSFER' AND toAccountId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfPayee = CursorUtil.getColumnIndexOrThrow(_cursor, "payee");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfToAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "toAccountId");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfModifiedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "modifiedAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfIsRecurring = CursorUtil.getColumnIndexOrThrow(_cursor, "isRecurring");
          final int _cursorIndexOfRecurringInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringInterval");
          final int _cursorIndexOfRecurringUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringUnit");
          final int _cursorIndexOfRecurringEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringEndDate");
          final int _cursorIndexOfParentRecurringId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentRecurringId");
          final int _cursorIndexOfNotifyForRecurringEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyForRecurringEntries");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpPayee;
            _tmpPayee = _cursor.getString(_cursorIndexOfPayee);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toTransactionType(_tmp);
            final String _tmpToAccountId;
            if (_cursor.isNull(_cursorIndexOfToAccountId)) {
              _tmpToAccountId = null;
            } else {
              _tmpToAccountId = _cursor.getString(_cursorIndexOfToAccountId);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpModifiedAt;
            _tmpModifiedAt = _cursor.getLong(_cursorIndexOfModifiedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final SyncStatus _tmpSyncStatus;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfSyncStatus);
            _tmpSyncStatus = __converters.toSyncStatus(_tmp_2);
            final boolean _tmpIsRecurring;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsRecurring);
            _tmpIsRecurring = _tmp_3 != 0;
            final Integer _tmpRecurringInterval;
            if (_cursor.isNull(_cursorIndexOfRecurringInterval)) {
              _tmpRecurringInterval = null;
            } else {
              _tmpRecurringInterval = _cursor.getInt(_cursorIndexOfRecurringInterval);
            }
            final RecurringUnit _tmpRecurringUnit;
            if (_cursor.isNull(_cursorIndexOfRecurringUnit)) {
              _tmpRecurringUnit = null;
            } else {
              _tmpRecurringUnit = __RecurringUnit_stringToEnum(_cursor.getString(_cursorIndexOfRecurringUnit));
            }
            final Long _tmpRecurringEndDate;
            if (_cursor.isNull(_cursorIndexOfRecurringEndDate)) {
              _tmpRecurringEndDate = null;
            } else {
              _tmpRecurringEndDate = _cursor.getLong(_cursorIndexOfRecurringEndDate);
            }
            final String _tmpParentRecurringId;
            if (_cursor.isNull(_cursorIndexOfParentRecurringId)) {
              _tmpParentRecurringId = null;
            } else {
              _tmpParentRecurringId = _cursor.getString(_cursorIndexOfParentRecurringId);
            }
            final boolean _tmpNotifyForRecurringEntries;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfNotifyForRecurringEntries);
            _tmpNotifyForRecurringEntries = _tmp_4 != 0;
            _item = new TransactionEntity(_tmpId,_tmpAccountId,_tmpPayee,_tmpNote,_tmpDate,_tmpTotalAmount,_tmpType,_tmpToAccountId,_tmpCreatedAt,_tmpModifiedAt,_tmpIsDeleted,_tmpSyncStatus,_tmpIsRecurring,_tmpRecurringInterval,_tmpRecurringUnit,_tmpRecurringEndDate,_tmpParentRecurringId,_tmpNotifyForRecurringEntries);
            _result.add(_item);
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

  private String __RecurringUnit_enumToString(@NonNull final RecurringUnit _value) {
    switch (_value) {
      case DAY: return "DAY";
      case WEEK: return "WEEK";
      case MONTH: return "MONTH";
      case YEAR: return "YEAR";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private void __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(
      @NonNull final ArrayMap<String, ArrayList<TransactionSplitEntity>> _map) {
    final Set<String> __mapKeySet = _map.keySet();
    if (__mapKeySet.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchArrayMap(_map, true, (map) -> {
        __fetchRelationshiptransactionSplitsAscomMoneytrackerAppDataLocalDatabaseEntitiesTransactionSplitEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`transactionId`,`categoryId`,`amount`,`note` FROM `transaction_splits` WHERE `transactionId` IN (");
    final int _inputSize = __mapKeySet.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : __mapKeySet) {
      _stmt.bindString(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "transactionId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfTransactionId = 1;
      final int _cursorIndexOfCategoryId = 2;
      final int _cursorIndexOfAmount = 3;
      final int _cursorIndexOfNote = 4;
      while (_cursor.moveToNext()) {
        final String _tmpKey;
        _tmpKey = _cursor.getString(_itemKeyIndex);
        final ArrayList<TransactionSplitEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final TransactionSplitEntity _item_1;
          final String _tmpId;
          _tmpId = _cursor.getString(_cursorIndexOfId);
          final String _tmpTransactionId;
          _tmpTransactionId = _cursor.getString(_cursorIndexOfTransactionId);
          final String _tmpCategoryId;
          _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
          final double _tmpAmount;
          _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
          final String _tmpNote;
          if (_cursor.isNull(_cursorIndexOfNote)) {
            _tmpNote = null;
          } else {
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
          }
          _item_1 = new TransactionSplitEntity(_tmpId,_tmpTransactionId,_tmpCategoryId,_tmpAmount,_tmpNote);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }

  private RecurringUnit __RecurringUnit_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "DAY": return RecurringUnit.DAY;
      case "WEEK": return RecurringUnit.WEEK;
      case "MONTH": return RecurringUnit.MONTH;
      case "YEAR": return RecurringUnit.YEAR;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
