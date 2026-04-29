package com.bettertick.di;

import android.content.Context;
import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideTaskRepositoryFactory implements Factory<TaskRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  private final Provider<Context> appContextProvider;

  public AppModule_ProvideTaskRepositoryFactory(Provider<FirestoreProvider> firestoreProvider,
      Provider<Context> appContextProvider) {
    this.firestoreProvider = firestoreProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public TaskRepository get() {
    return provideTaskRepository(firestoreProvider.get(), appContextProvider.get());
  }

  public static AppModule_ProvideTaskRepositoryFactory create(
      Provider<FirestoreProvider> firestoreProvider, Provider<Context> appContextProvider) {
    return new AppModule_ProvideTaskRepositoryFactory(firestoreProvider, appContextProvider);
  }

  public static TaskRepository provideTaskRepository(FirestoreProvider firestoreProvider,
      Context appContext) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTaskRepository(firestoreProvider, appContext));
  }
}
