package com.moneytracker.app.ui.screens.settings;

import com.moneytracker.app.data.local.UserPreferences;
import com.moneytracker.app.data.local.repository.AccountRepository;
import com.moneytracker.app.data.local.repository.BudgetRepository;
import com.moneytracker.app.data.local.repository.CategoryRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<BudgetRepository> budgetRepositoryProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public SettingsViewModel_Factory(Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<BudgetRepository> budgetRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.budgetRepositoryProvider = budgetRepositoryProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(transactionRepositoryProvider.get(), accountRepositoryProvider.get(), categoryRepositoryProvider.get(), budgetRepositoryProvider.get(), userPreferencesProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<BudgetRepository> budgetRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new SettingsViewModel_Factory(transactionRepositoryProvider, accountRepositoryProvider, categoryRepositoryProvider, budgetRepositoryProvider, userPreferencesProvider);
  }

  public static SettingsViewModel newInstance(TransactionRepository transactionRepository,
      AccountRepository accountRepository, CategoryRepository categoryRepository,
      BudgetRepository budgetRepository, UserPreferences userPreferences) {
    return new SettingsViewModel(transactionRepository, accountRepository, categoryRepository, budgetRepository, userPreferences);
  }
}
