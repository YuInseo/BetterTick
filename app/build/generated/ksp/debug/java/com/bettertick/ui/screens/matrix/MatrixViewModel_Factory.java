package com.bettertick.ui.screens.matrix;

import com.bettertick.data.repository.ListRepository;
import com.bettertick.data.repository.MatrixRepository;
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
public final class MatrixViewModel_Factory implements Factory<MatrixViewModel> {
  private final Provider<MatrixRepository> matrixRepositoryProvider;

  private final Provider<TaskRepository> taskRepositoryProvider;

  private final Provider<ListRepository> listRepositoryProvider;

  private final Provider<TagRepository> tagRepositoryProvider;

  public MatrixViewModel_Factory(Provider<MatrixRepository> matrixRepositoryProvider,
      Provider<TaskRepository> taskRepositoryProvider,
      Provider<ListRepository> listRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider) {
    this.matrixRepositoryProvider = matrixRepositoryProvider;
    this.taskRepositoryProvider = taskRepositoryProvider;
    this.listRepositoryProvider = listRepositoryProvider;
    this.tagRepositoryProvider = tagRepositoryProvider;
  }

  @Override
  public MatrixViewModel get() {
    return newInstance(matrixRepositoryProvider.get(), taskRepositoryProvider.get(), listRepositoryProvider.get(), tagRepositoryProvider.get());
  }

  public static MatrixViewModel_Factory create(Provider<MatrixRepository> matrixRepositoryProvider,
      Provider<TaskRepository> taskRepositoryProvider,
      Provider<ListRepository> listRepositoryProvider,
      Provider<TagRepository> tagRepositoryProvider) {
    return new MatrixViewModel_Factory(matrixRepositoryProvider, taskRepositoryProvider, listRepositoryProvider, tagRepositoryProvider);
  }

  public static MatrixViewModel newInstance(MatrixRepository matrixRepository,
      TaskRepository taskRepository, ListRepository listRepository, TagRepository tagRepository) {
    return new MatrixViewModel(matrixRepository, taskRepository, listRepository, tagRepository);
  }
}
