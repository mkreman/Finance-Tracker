package com.moneytracker.app.ui.screens.accounts;

import com.moneytracker.app.data.local.UserPreferences;
import com.moneytracker.app.data.local.repository.AccountRepository;
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
public final class AccountsViewModel_Factory implements Factory<AccountsViewModel> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public AccountsViewModel_Factory(Provider<AccountRepository> accountRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  @Override
  public AccountsViewModel get() {
    return newInstance(accountRepositoryProvider.get(), userPreferencesProvider.get());
  }

  public static AccountsViewModel_Factory create(
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new AccountsViewModel_Factory(accountRepositoryProvider, userPreferencesProvider);
  }

  public static AccountsViewModel newInstance(AccountRepository accountRepository,
      UserPreferences userPreferences) {
    return new AccountsViewModel(accountRepository, userPreferences);
  }
}
