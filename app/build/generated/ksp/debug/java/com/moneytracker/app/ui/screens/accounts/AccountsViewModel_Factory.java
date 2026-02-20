package com.moneytracker.app.ui.screens.accounts;

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

  public AccountsViewModel_Factory(Provider<AccountRepository> accountRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
  }

  @Override
  public AccountsViewModel get() {
    return newInstance(accountRepositoryProvider.get());
  }

  public static AccountsViewModel_Factory create(
      Provider<AccountRepository> accountRepositoryProvider) {
    return new AccountsViewModel_Factory(accountRepositoryProvider);
  }

  public static AccountsViewModel newInstance(AccountRepository accountRepository) {
    return new AccountsViewModel(accountRepository);
  }
}
