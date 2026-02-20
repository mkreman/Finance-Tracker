package com.moneytracker.app.data.local.repository;

import com.moneytracker.app.data.local.database.dao.AccountDao;
import com.moneytracker.app.data.local.database.dao.TransactionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AccountRepository_Factory implements Factory<AccountRepository> {
  private final Provider<AccountDao> accountDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  public AccountRepository_Factory(Provider<AccountDao> accountDaoProvider,
      Provider<TransactionDao> transactionDaoProvider) {
    this.accountDaoProvider = accountDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
  }

  @Override
  public AccountRepository get() {
    return newInstance(accountDaoProvider.get(), transactionDaoProvider.get());
  }

  public static AccountRepository_Factory create(Provider<AccountDao> accountDaoProvider,
      Provider<TransactionDao> transactionDaoProvider) {
    return new AccountRepository_Factory(accountDaoProvider, transactionDaoProvider);
  }

  public static AccountRepository newInstance(AccountDao accountDao,
      TransactionDao transactionDao) {
    return new AccountRepository(accountDao, transactionDao);
  }
}
