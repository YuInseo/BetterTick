package com.bettertick.data.repository;

import com.bettertick.data.firebase.FirestoreProvider;
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
public final class TabBarRepository_Factory implements Factory<TabBarRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public TabBarRepository_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public TabBarRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static TabBarRepository_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new TabBarRepository_Factory(firestoreProvider);
  }

  public static TabBarRepository newInstance(FirestoreProvider firestoreProvider) {
    return new TabBarRepository(firestoreProvider);
  }
}
