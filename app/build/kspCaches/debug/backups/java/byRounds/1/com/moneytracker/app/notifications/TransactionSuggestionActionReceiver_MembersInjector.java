package com.moneytracker.app.notifications;

import com.moneytracker.app.data.local.UserPreferences;
import com.moneytracker.app.data.local.repository.AccountRepository;
import com.moneytracker.app.data.local.repository.CategoryRepository;
import com.moneytracker.app.data.local.repository.TransactionRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TransactionSuggestionActionReceiver_MembersInjector implements MembersInjector<TransactionSuggestionActionReceiver> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<AccountRepository> accountRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public TransactionSuggestionActionReceiver_MembersInjector(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  public static MembersInjector<TransactionSuggestionActionReceiver> create(
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<AccountRepository> accountRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new TransactionSuggestionActionReceiver_MembersInjector(transactionRepositoryProvider, accountRepositoryProvider, categoryRepositoryProvider, userPreferencesProvider);
  }

  @Override
  public void injectMembers(TransactionSuggestionActionReceiver instance) {
    injectTransactionRepository(instance, transactionRepositoryProvider.get());
    injectAccountRepository(instance, accountRepositoryProvider.get());
    injectCategoryRepository(instance, categoryRepositoryProvider.get());
    injectUserPreferences(instance, userPreferencesProvider.get());
  }

  @InjectedFieldSignature("com.moneytracker.app.notifications.TransactionSuggestionActionReceiver.transactionRepository")
  public static void injectTransactionRepository(TransactionSuggestionActionReceiver instance,
      TransactionRepository transactionRepository) {
    instance.transactionRepository = transactionRepository;
  }

  @InjectedFieldSignature("com.moneytracker.app.notifications.TransactionSuggestionActionReceiver.accountRepository")
  public static void injectAccountRepository(TransactionSuggestionActionReceiver instance,
      AccountRepository accountRepository) {
    instance.accountRepository = accountRepository;
  }

  @InjectedFieldSignature("com.moneytracker.app.notifications.TransactionSuggestionActionReceiver.categoryRepository")
  public static void injectCategoryRepository(TransactionSuggestionActionReceiver instance,
      CategoryRepository categoryRepository) {
    instance.categoryRepository = categoryRepository;
  }

  @InjectedFieldSignature("com.moneytracker.app.notifications.TransactionSuggestionActionReceiver.userPreferences")
  public static void injectUserPreferences(TransactionSuggestionActionReceiver instance,
      UserPreferences userPreferences) {
    instance.userPreferences = userPreferences;
  }
}
