package com.moneytracker.app.ui.screens.budget;

import com.moneytracker.app.data.local.repository.BudgetRepository;
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
public final class BudgetViewModel_Factory implements Factory<BudgetViewModel> {
  private final Provider<BudgetRepository> budgetRepositoryProvider;

  public BudgetViewModel_Factory(Provider<BudgetRepository> budgetRepositoryProvider) {
    this.budgetRepositoryProvider = budgetRepositoryProvider;
  }

  @Override
  public BudgetViewModel get() {
    return newInstance(budgetRepositoryProvider.get());
  }

  public static BudgetViewModel_Factory create(
      Provider<BudgetRepository> budgetRepositoryProvider) {
    return new BudgetViewModel_Factory(budgetRepositoryProvider);
  }

  public static BudgetViewModel newInstance(BudgetRepository budgetRepository) {
    return new BudgetViewModel(budgetRepository);
  }
}
