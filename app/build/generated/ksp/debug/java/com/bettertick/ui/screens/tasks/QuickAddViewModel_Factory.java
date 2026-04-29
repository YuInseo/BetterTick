package com.bettertick.ui.screens.tasks;

import android.content.Context;
import com.bettertick.data.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class QuickAddViewModel_Factory implements Factory<QuickAddViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<TaskRepository> taskRepositoryProvider;

  public QuickAddViewModel_Factory(Provider<Context> contextProvider,
      Provider<TaskRepository> taskRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.taskRepositoryProvider = taskRepositoryProvider;
  }

  @Override
  public QuickAddViewModel get() {
    return newInstance(contextProvider.get(), taskRepositoryProvider.get());
  }

  public static QuickAddViewModel_Factory create(Provider<Context> contextProvider,
      Provider<TaskRepository> taskRepositoryProvider) {
    return new QuickAddViewModel_Factory(contextProvider, taskRepositoryProvider);
  }

  public static QuickAddViewModel newInstance(Context context, TaskRepository taskRepository) {
    return new QuickAddViewModel(context, taskRepository);
  }
}
