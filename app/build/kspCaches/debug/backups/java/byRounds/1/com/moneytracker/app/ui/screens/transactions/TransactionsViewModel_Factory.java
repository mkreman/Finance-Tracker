package com.moneytracker.app.ui.screens.transactions;

import com.moneytracker.app.data.local.repository.TransactionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TransactionsViewModel_Factory implements Factory<TransactionsViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public TransactionsViewModel_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public TransactionsViewModel get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static TransactionsViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new TransactionsViewModel_Factory(transactionRepositoryProvider);
  }

  public static TransactionsViewModel newInstance(TransactionRepository transactionRepository) {
    return new TransactionsViewModel(transactionRepository);
  }
}
