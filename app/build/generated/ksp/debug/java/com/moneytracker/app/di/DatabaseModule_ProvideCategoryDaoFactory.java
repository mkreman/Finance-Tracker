package com.moneytracker.app.di;

import com.moneytracker.app.data.local.database.MoneyTrackerDatabase;
import com.moneytracker.app.data.local.database.dao.CategoryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideCategoryDaoFactory implements Factory<CategoryDao> {
  private final Provider<MoneyTrackerDatabase> databaseProvider;

  public DatabaseModule_ProvideCategoryDaoFactory(Provider<MoneyTrackerDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CategoryDao get() {
    return provideCategoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideCategoryDaoFactory create(
      Provider<MoneyTrackerDatabase> databaseProvider) {
    return new DatabaseModule_ProvideCategoryDaoFactory(databaseProvider);
  }

  public static CategoryDao provideCategoryDao(MoneyTrackerDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCategoryDao(database));
  }
}
