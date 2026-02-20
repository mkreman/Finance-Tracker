package com.moneytracker.app.data.recurring;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class RecurringTransactionManager_Factory implements Factory<RecurringTransactionManager> {
  private final Provider<Context> contextProvider;

  public RecurringTransactionManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RecurringTransactionManager get() {
    return newInstance(contextProvider.get());
  }

  public static RecurringTransactionManager_Factory create(Provider<Context> contextProvider) {
    return new RecurringTransactionManager_Factory(contextProvider);
  }

  public static RecurringTransactionManager newInstance(Context context) {
    return new RecurringTransactionManager(context);
  }
}
