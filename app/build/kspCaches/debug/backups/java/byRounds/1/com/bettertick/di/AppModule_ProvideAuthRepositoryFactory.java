package com.bettertick.di;

import com.bettertick.data.firebase.DataSeeder;
import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirestoreProvider> firestoreProvider;

  private final Provider<DataSeeder> dataSeederProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<FirebaseAuth> authProvider,
      Provider<FirestoreProvider> firestoreProvider, Provider<DataSeeder> dataSeederProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
    this.dataSeederProvider = dataSeederProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(authProvider.get(), firestoreProvider.get(), dataSeederProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(Provider<FirebaseAuth> authProvider,
      Provider<FirestoreProvider> firestoreProvider, Provider<DataSeeder> dataSeederProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(authProvider, firestoreProvider, dataSeederProvider);
  }

  public static AuthRepository provideAuthRepository(FirebaseAuth auth,
      FirestoreProvider firestoreProvider, DataSeeder dataSeeder) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(auth, firestoreProvider, dataSeeder));
  }
}
