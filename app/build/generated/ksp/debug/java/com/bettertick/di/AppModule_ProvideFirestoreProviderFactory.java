package com.bettertick.di;

import com.bettertick.data.firebase.FirestoreProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
public final class AppModule_ProvideFirestoreProviderFactory implements Factory<FirestoreProvider> {
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  public AppModule_ProvideFirestoreProviderFactory(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public FirestoreProvider get() {
    return provideFirestoreProvider(authProvider.get(), firestoreProvider.get());
  }

  public static AppModule_ProvideFirestoreProviderFactory create(
      Provider<FirebaseAuth> authProvider, Provider<FirebaseFirestore> firestoreProvider) {
    return new AppModule_ProvideFirestoreProviderFactory(authProvider, firestoreProvider);
  }

  public static FirestoreProvider provideFirestoreProvider(FirebaseAuth auth,
      FirebaseFirestore firestore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFirestoreProvider(auth, firestore));
  }
}
