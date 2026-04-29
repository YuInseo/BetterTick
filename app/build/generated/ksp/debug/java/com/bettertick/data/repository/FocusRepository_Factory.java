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
public final class FocusRepository_Factory implements Factory<FocusRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public FocusRepository_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public FocusRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static FocusRepository_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new FocusRepository_Factory(firestoreProvider);
  }

  public static FocusRepository newInstance(FirestoreProvider firestoreProvider) {
    return new FocusRepository(firestoreProvider);
  }
}
