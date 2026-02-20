package com.moneytracker.app.workers;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class RecurringTransactionWorker_AssistedFactory_Impl implements RecurringTransactionWorker_AssistedFactory {
  private final RecurringTransactionWorker_Factory delegateFactory;

  RecurringTransactionWorker_AssistedFactory_Impl(
      RecurringTransactionWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public RecurringTransactionWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<RecurringTransactionWorker_AssistedFactory> create(
      RecurringTransactionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new RecurringTransactionWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<RecurringTransactionWorker_AssistedFactory> createFactoryProvider(
      RecurringTransactionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new RecurringTransactionWorker_AssistedFactory_Impl(delegateFactory));
  }
}
