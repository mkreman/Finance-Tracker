package com.moneytracker.app.ui.screens.dashboard;

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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public DashboardViewModel_Factory(Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new DashboardViewModel_Factory(transactionRepositoryProvider);
  }

  public static DashboardViewModel newInstance(TransactionRepository transactionRepository) {
    return new DashboardViewModel(transactionRepository);
  }
}
