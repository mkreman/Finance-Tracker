package com.moneytracker.app.data.local;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class UserPreferences_Factory implements Factory<UserPreferences> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public UserPreferences_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public UserPreferences get() {
    return newInstance(dataStoreProvider.get());
  }

  public static UserPreferences_Factory create(Provider<DataStore<Preferences>> dataStoreProvider) {
    return new UserPreferences_Factory(dataStoreProvider);
  }

  public static UserPreferences newInstance(DataStore<Preferences> dataStore) {
    return new UserPreferences(dataStore);
  }
}
