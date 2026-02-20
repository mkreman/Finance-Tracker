package com.moneytracker.app.ui.screens.accounts;

import androidx.lifecycle.SavedStateHandle;
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
public final class AddAccountViewModel_Factory implements Factory<AddAccountViewModel> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AddAccountViewModel_Factory(Provider<AccountRepository> accountRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AddAccountViewModel get() {
    return newInstance(accountRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static AddAccountViewModel_Factory create(
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AddAccountViewModel_Factory(accountRepositoryProvider, savedStateHandleProvider);
  }

  public static AddAccountViewModel newInstance(AccountRepository accountRepository,
      SavedStateHandle savedStateHandle) {
    return new AddAccountViewModel(accountRepository, savedStateHandle);
  }
}
