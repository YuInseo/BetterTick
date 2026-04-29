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
public final class MatrixRepository_Factory implements Factory<MatrixRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public MatrixRepository_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public MatrixRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static MatrixRepository_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new MatrixRepository_Factory(firestoreProvider);
  }

  public static MatrixRepository newInstance(FirestoreProvider firestoreProvider) {
    return new MatrixRepository(firestoreProvider);
  }
}
