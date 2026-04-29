package com.bettertick.ui.screens.calendar;

import com.bettertick.data.repository.ListRepository;
import com.bettertick.data.repository.TagRepository;
import com.bettertick.data.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class CalendarViewModel_Factory implements Factory<CalendarViewModel> {
  private final Provider<TaskRepository> taskRepositoryProvider;

  private final Provider<ListRepository> listRepositoryProvider;

  private final Provider<TagRepository> tagRepositoryProvider;

  public CalendarViewModel_Factory(Provider<TaskRepository> taskRepositoryProvider,
      Provider<ListRepository> listRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider) {
    this.taskRepositoryProvider = taskRepositoryProvider;
    this.listRepositoryProvider = listRepositoryProvider;
    this.tagRepositoryProvider = tagRepositoryProvider;
  }

  @Override
  public CalendarViewModel get() {
    return newInstance(taskRepositoryProvider.get(), listRepositoryProvider.get(), tagRepositoryProvider.get());
  }

  public static CalendarViewModel_Factory create(Provider<TaskRepository> taskRepositoryProvider,
      Provider<ListRepository> listRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider) {
    return new CalendarViewModel_Factory(taskRepositoryProvider, listRepositoryProvider, tagRepositoryProvider);
  }

  public static CalendarViewModel newInstance(TaskRepository taskRepository,
      ListRepository listRepository, TagRepository tagRepository) {
    return new CalendarViewModel(taskRepository, listRepository, tagRepository);
  }
}
