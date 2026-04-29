package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.TagRepository;
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
public final class AppModule_ProvideTagRepositoryFactory implements Factory<TagRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideTagRepositoryFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public TagRepository get() {
    return provideTagRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideTagRepositoryFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideTagRepositoryFactory(firestoreProvider);
  }

  public static TagRepository provideTagRepository(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTagRepository(firestoreProvider));
  }
}
