package com.moneytracker.app;

import androidx.hilt.work.HiltWorkerFactory;
import com.moneytracker.app.data.sync.SyncManager;
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
public final class MoneyTrackerApp_MembersInjector implements MembersInjector<MoneyTrackerApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<SyncManager> syncManagerProvider;

  public MoneyTrackerApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<SyncManager> syncManagerProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.syncManagerProvider = syncManagerProvider;
  }

  public static MembersInjector<MoneyTrackerApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<SyncManager> syncManagerProvider) {
    return new MoneyTrackerApp_MembersInjector(workerFactoryProvider, syncManagerProvider);
  }

  @Override
  public void injectMembers(MoneyTrackerApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectSyncManager(instance, syncManagerProvider.get());
  }

  @InjectedFieldSignature("com.moneytracker.app.MoneyTrackerApp.workerFactory")
  public static void injectWorkerFactory(MoneyTrackerApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.moneytracker.app.MoneyTrackerApp.syncManager")
  public static void injectSyncManager(MoneyTrackerApp instance, SyncManager syncManager) {
    instance.syncManager = syncManager;
  }
}
