package com.moneytracker.app.data.local.repository;

import com.moneytracker.app.data.local.database.dao.CategoryDao;
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
public final class CategoryRepository_Factory implements Factory<CategoryRepository> {
  private final Provider<CategoryDao> categoryDaoProvider;

  public CategoryRepository_Factory(Provider<CategoryDao> categoryDaoProvider) {
    this.categoryDaoProvider = categoryDaoProvider;
  }

  @Override
  public CategoryRepository get() {
    return newInstance(categoryDaoProvider.get());
  }

  public static CategoryRepository_Factory create(Provider<CategoryDao> categoryDaoProvider) {
    return new CategoryRepository_Factory(categoryDaoProvider);
  }

  public static CategoryRepository newInstance(CategoryDao categoryDao) {
    return new CategoryRepository(categoryDao);
  }
}
