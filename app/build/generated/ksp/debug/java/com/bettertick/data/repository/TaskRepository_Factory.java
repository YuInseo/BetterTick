package com.bettertick.data.repository;

import android.content.Context;
import com.bettertick.data.firebase.FirestoreProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TaskRepository_Factory implements Factory<TaskRepository> {
  private final Provider<FirestoreProvider> firestoreProvider;

  private final Provider<Context> appContextProvider;

  public TaskRepository_Factory(Provider<FirestoreProvider> firestoreProvider,
      Provider<Context> appContextProvider) {
    this.firestoreProvider = firestoreProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public TaskRepository get() {
    return newInstance(firestoreProvider.get(), appContextProvider.get());
  }

  public static TaskRepository_Factory create(Provider<FirestoreProvider> firestoreProvider,
      Provider<Context> appContextProvider) {
    return new TaskRepository_Factory(firestoreProvider, appContextProvider);
  }

  public static TaskRepository newInstance(FirestoreProvider firestoreProvider,
      Context appContext) {
    return new TaskRepository(firestoreProvider, appContext);
  }
}
