package com.moneytracker.app.ui.screens.transactions;

import androidx.lifecycle.SavedStateHandle;
import com.moneytracker.app.data.local.UserPreferences;
import com.moneytracker.app.data.local.repository.AccountRepository;
import com.moneytracker.app.data.local.repository.CategoryRepository;
import com.moneytracker.app.data.local.repository.TransactionRepository;
import com.moneytracker.app.data.recurring.RecurringTransactionManager;
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
public final class AddTransactionViewModel_Factory implements Factory<AddTransactionViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  private final Provider<RecurringTransactionManager> recurringTransactionManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AddTransactionViewModel_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider,
      Provider<RecurringTransactionManager> recurringTransactionManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.userPreferencesProvider = userPreferencesProvider;
    this.recurringTransactionManagerProvider = recurringTransactionManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AddTransactionViewModel get() {
    return newInstance(transactionRepositoryProvider.get(), accountRepositoryProvider.get(), categoryRepositoryProvider.get(), userPreferencesProvider.get(), recurringTransactionManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static AddTransactionViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider,
      Provider<RecurringTransactionManager> recurringTransactionManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AddTransactionViewModel_Factory(transactionRepositoryProvider, accountRepositoryProvider, categoryRepositoryProvider, userPreferencesProvider, recurringTransactionManagerProvider, savedStateHandleProvider);
  }

  public static AddTransactionViewModel newInstance(TransactionRepository transactionRepository,
      AccountRepository accountRepository, CategoryRepository categoryRepository,
      UserPreferences userPreferences, RecurringTransactionManager recurringTransactionManager,
      SavedStateHandle savedStateHandle) {
    return new AddTransactionViewModel(transactionRepository, accountRepository, categoryRepository, userPreferences, recurringTransactionManager, savedStateHandle);
  }
}
