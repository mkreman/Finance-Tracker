package com.moneytracker.app.ui.screens.dashboard;

import androidx.lifecycle.SavedStateHandle;
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
public final class CategoryTransactionsViewModel_Factory implements Factory<CategoryTransactionsViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public CategoryTransactionsViewModel_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public CategoryTransactionsViewModel get() {
    return newInstance(transactionRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static CategoryTransactionsViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new CategoryTransactionsViewModel_Factory(transactionRepositoryProvider, savedStateHandleProvider);
  }

  public static CategoryTransactionsViewModel newInstance(
      TransactionRepository transactionRepository, SavedStateHandle savedStateHandle) {
    return new CategoryTransactionsViewModel(transactionRepository, savedStateHandle);
  }
}
