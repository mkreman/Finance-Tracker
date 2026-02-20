package com.moneytracker.app.data.sync;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.moneytracker.app.data.local.database.dao.TransactionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class SyncWorker_Factory {
  private final Provider<TransactionDao> transactionDaoProvider;

  public SyncWorker_Factory(Provider<TransactionDao> transactionDaoProvider) {
    this.transactionDaoProvider = transactionDaoProvider;
  }

  public SyncWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, transactionDaoProvider.get());
  }

  public static SyncWorker_Factory create(Provider<TransactionDao> transactionDaoProvider) {
    return new SyncWorker_Factory(transactionDaoProvider);
  }

  public static SyncWorker newInstance(Context context, WorkerParameters params,
      TransactionDao transactionDao) {
    return new SyncWorker(context, params, transactionDao);
  }
}
