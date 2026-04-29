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
public final class UserRepository_Factory implements Factory<UserRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public UserRepository_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public UserRepository get() {
    return newInstance(firestoreProvider.get());
  }

  public static UserRepository_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new UserRepository_Factory(firestoreProvider);
  }

  public static UserRepository newInstance(FirestoreProvider firestoreProvider) {
    return new UserRepository(firestoreProvider);
  }
}
