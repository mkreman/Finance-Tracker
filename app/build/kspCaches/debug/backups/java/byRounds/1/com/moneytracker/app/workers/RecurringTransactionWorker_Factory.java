package com.moneytracker.app.workers;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.moneytracker.app.data.local.repository.TransactionRepository;
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
public final class RecurringTransactionWorker_Factory {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public RecurringTransactionWorker_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  public RecurringTransactionWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, transactionRepositoryProvider.get());
  }

  public static RecurringTransactionWorker_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new RecurringTransactionWorker_Factory(transactionRepositoryProvider);
  }

  public static RecurringTransactionWorker newInstance(Context context, WorkerParameters params,
      TransactionRepository transactionRepository) {
    return new RecurringTransactionWorker(context, params, transactionRepository);
  }
}
