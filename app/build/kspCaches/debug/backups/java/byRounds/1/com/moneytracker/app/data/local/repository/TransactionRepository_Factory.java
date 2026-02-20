package com.moneytracker.app.data.local.repository;

import com.moneytracker.app.data.local.database.MoneyTrackerDatabase;
import com.moneytracker.app.data.local.database.dao.AccountDao;
import com.moneytracker.app.data.local.database.dao.CategoryDao;
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
public final class TransactionRepository_Factory implements Factory<TransactionRepository> {
  private final Provider<MoneyTrackerDatabase> databaseProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<AccountDao> accountDaoProvider;

  public TransactionRepository_Factory(Provider<MoneyTrackerDatabase> databaseProvider,
      Provider<TransactionDao> transactionDaoProvider, Provider<CategoryDao> categoryDaoProvider,
      Provider<AccountDao> accountDaoProvider) {
    this.databaseProvider = databaseProvider;
    this.transactionDaoProvider = transactionDaoProvider;
    this.categoryDaoProvider = categoryDaoProvider;
    this.accountDaoProvider = accountDaoProvider;
  }

  @Override
  public TransactionRepository get() {
    return newInstance(databaseProvider.get(), transactionDaoProvider.get(), categoryDaoProvider.get(), accountDaoProvider.get());
  }

  public static TransactionRepository_Factory create(
      Provider<MoneyTrackerDatabase> databaseProvider,
      Provider<TransactionDao> transactionDaoProvider, Provider<CategoryDao> categoryDaoProvider,
      Provider<AccountDao> accountDaoProvider) {
    return new TransactionRepository_Factory(databaseProvider, transactionDaoProvider, categoryDaoProvider, accountDaoProvider);
  }

  public static TransactionRepository newInstance(MoneyTrackerDatabase database,
      TransactionDao transactionDao, CategoryDao categoryDao, AccountDao accountDao) {
    return new TransactionRepository(database, transactionDao, categoryDao, accountDao);
  }
}
