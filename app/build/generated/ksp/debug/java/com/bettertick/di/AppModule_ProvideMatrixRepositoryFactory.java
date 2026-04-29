package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.MatrixRepository;
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
public final class AppModule_ProvideMatrixRepositoryFactory implements Factory<MatrixRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideMatrixRepositoryFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public MatrixRepository get() {
    return provideMatrixRepository(firestoreProvider.get());
  }

  public static AppModule_ProvideMatrixRepositoryFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideMatrixRepositoryFactory(firestoreProvider);
  }

  public static MatrixRepository provideMatrixRepository(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMatrixRepository(firestoreProvider));
  }
}
