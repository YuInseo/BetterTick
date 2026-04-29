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
public final class ListRepository_Factory implements Factory<ListRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public ListRepository_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public ListRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static ListRepository_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new ListRepository_Factory(firestoreProvider);
  }

  public static ListRepository newInstance(FirestoreProvider firestoreProvider) {
    return new ListRepository(firestoreProvider);
  }
}
