package com.moneytracker.app.ui.screens.budget;

import androidx.lifecycle.SavedStateHandle;
import com.moneytracker.app.data.local.repository.BudgetRepository;
import com.moneytracker.app.data.local.repository.CategoryRepository;
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
public final class AddBudgetViewModel_Factory implements Factory<AddBudgetViewModel> {
  private final Provider<BudgetRepository> budgetRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AddBudgetViewModel_Factory(Provider<BudgetRepository> budgetRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.budgetRepositoryProvider = budgetRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AddBudgetViewModel get() {
    return newInstance(budgetRepositoryProvider.get(), categoryRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static AddBudgetViewModel_Factory create(
      Provider<BudgetRepository> budgetRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AddBudgetViewModel_Factory(budgetRepositoryProvider, categoryRepositoryProvider, savedStateHandleProvider);
  }

  public static AddBudgetViewModel newInstance(BudgetRepository budgetRepository,
      CategoryRepository categoryRepository, SavedStateHandle savedStateHandle) {
    return new AddBudgetViewModel(budgetRepository, categoryRepository, savedStateHandle);
  }
}
