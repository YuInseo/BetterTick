package com.bettertick.data.repository;

import com.bettertick.data.firebase.DataSeeder;
import com.bettertick.data.firebase.FirestoreProvider;
import com.google.firebase.auth.FirebaseAuth;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirestoreProvider> firestoreProvider;

  private final Provider<DataSeeder> dataSeederProvider;

  public AuthRepository_Factory(Provider<FirebaseAuth> authProvider,
      Provider<FirestoreProvider> firestoreProvider, Provider<DataSeeder> dataSeederProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
    this.dataSeederProvider = dataSeederProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(authProvider.get(), firestoreProvider.get(), dataSeederProvider.get());
  }

  public static AuthRepository_Factory create(Provider<FirebaseAuth> authProvider,
      Provider<FirestoreProvider> firestoreProvider, Provider<DataSeeder> dataSeederProvider) {
    return new AuthRepository_Factory(authProvider, firestoreProvider, dataSeederProvider);
  }

  public static AuthRepository newInstance(FirebaseAuth auth, FirestoreProvider firestoreProvider,
      DataSeeder dataSeeder) {
    return new AuthRepository(auth, firestoreProvider, dataSeeder);
  }
}
