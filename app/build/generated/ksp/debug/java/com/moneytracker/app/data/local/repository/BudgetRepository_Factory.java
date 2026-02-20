package com.moneytracker.app.data.local.repository;

import com.moneytracker.app.data.local.database.dao.BudgetDao;
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
public final class BudgetRepository_Factory implements Factory<BudgetRepository> {
  private final Provider<BudgetDao> budgetDaoProvider;

  public BudgetRepository_Factory(Provider<BudgetDao> budgetDaoProvider) {
    this.budgetDaoProvider = budgetDaoProvider;
  }

  @Override
  public BudgetRepository get() {
    return newInstance(budgetDaoProvider.get());
  }

  public static BudgetRepository_Factory create(Provider<BudgetDao> budgetDaoProvider) {
    return new BudgetRepository_Factory(budgetDaoProvider);
  }

  public static BudgetRepository newInstance(BudgetDao budgetDao) {
    return new BudgetRepository(budgetDao);
  }
}
