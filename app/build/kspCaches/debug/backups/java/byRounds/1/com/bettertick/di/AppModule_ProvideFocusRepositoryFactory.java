package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.FocusRepository;
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
public final class AppModule_ProvideFocusRepositoryFactory implements Factory<FocusRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideFocusRepositoryFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public FocusRepository get() {
    return provideFocusRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideFocusRepositoryFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideFocusRepositoryFactory(firestoreProvider);
  }

  public static FocusRepository provideFocusRepository(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFocusRepository(firestoreProvider));
  }
}
