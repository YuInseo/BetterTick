package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.TabBarRepository;
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
public final class AppModule_ProvideTabBarRepositoryFactory implements Factory<TabBarRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideTabBarRepositoryFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public TabBarRepository get() {
    return provideTabBarRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideTabBarRepositoryFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideTabBarRepositoryFactory(firestoreProvider);
  }

  public static TabBarRepository provideTabBarRepository(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTabBarRepository(firestoreProvider));
  }
}
