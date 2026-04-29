package com.bettertick.di;

import com.bettertick.data.firebase.DataSeeder;
import com.bettertick.data.firebase.FirestoreProvider;
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
public final class AppModule_ProvideDataSeederFactory implements Factory<DataSeeder> {
  private final Provider<FirestoreProvider> firestoreProvider;

  public AppModule_ProvideDataSeederFactory(Provider<FirestoreProvider> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public DataSeeder get() {
    return provideDataSeeder(firestoreProvider.get());
  }

  public static AppModule_ProvideDataSeederFactory create(
      Provider<FirestoreProvider> firestoreProvider) {
    return new AppModule_ProvideDataSeederFactory(firestoreProvider);
  }

  public static DataSeeder provideDataSeeder(FirestoreProvider firestoreProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDataSeeder(firestoreProvider));
  }
}
