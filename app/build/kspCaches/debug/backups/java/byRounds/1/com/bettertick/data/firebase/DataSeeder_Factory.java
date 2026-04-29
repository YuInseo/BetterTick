package com.bettertick.data.firebase;

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
    "KotlinInternalInJava",
    "cast"
})
public final class DataSeeder_Factory implements Factory<DataSeeder> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public DataSeeder_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public DataSeeder get() {
    return newInstance(firestoreProvider.get());
  }

  public static DataSeeder_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new DataSeeder_Factory(firestoreProvider);
  }

  public static DataSeeder newInstance(FirestoreProvider firestoreProvider) {
    return new DataSeeder(firestoreProvider);
  }
}
