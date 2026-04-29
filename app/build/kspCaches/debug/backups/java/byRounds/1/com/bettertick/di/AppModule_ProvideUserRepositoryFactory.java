package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.UserRepository;
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
public final class AppModule_ProvideUserRepositoryFactory implements Factory<UserRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideUserRepositoryFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public UserRepository get() {
    return provideUserRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideUserRepositoryFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideUserRepositoryFactory(firestoreProvider);
  }

  public static UserRepository provideUserRepository(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserRepository(firestoreProvider));
  }
}
