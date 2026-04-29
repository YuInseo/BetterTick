package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.firebase.SyncManager;
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
public final class AppModule_ProvideSyncManagerFactory implements Factory<SyncManager> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideSyncManagerFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public SyncManager get() {
    return provideSyncManager(firestoreProvider.get());
  }

  public static AppModule_ProvideSyncManagerFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideSyncManagerFactory(firestoreProvider);
  }

  public static SyncManager provideSyncManager(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSyncManager(firestoreProvider));
  }
}
