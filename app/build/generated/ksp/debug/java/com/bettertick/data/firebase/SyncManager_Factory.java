package com.bettertick.data.firebase;

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
public final class SyncManager_Factory implements Factory<SyncManager> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public SyncManager_Factory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public SyncManager get() {
    return newInstance(firestoreProvider.get());
  }

  public static SyncManager_Factory create(Provider<FirestoreProvider> firestoreProvider) {
    return new SyncManager_Factory(firestoreProvider);
  }

  public static SyncManager newInstance(FirestoreProvider firestoreProvider) {
    return new SyncManager(firestoreProvider);
  }
}
