package com.bettertick.ui.screens.tags;

import com.bettertick.data.repository.TagRepository;
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
public final class AddTagViewModel_Factory implements Factory<AddTagViewModel> {
  private final Provider<TagRepository> tagRepositoryProvider;

  public AddTagViewModel_Factory(Provider<TagRepository> tagRepositoryProvider) {
    this.tagRepositoryProvider = tagRepositoryProvider;
  }

  @Override
  public AddTagViewModel get() {
    return newInstance(tagRepositoryProvider.get());
  }

  public static AddTagViewModel_Factory create(Provider<TagRepository> tagRepositoryProvider) {
    return new AddTagViewModel_Factory(tagRepositoryProvider);
  }

  public static AddTagViewModel newInstance(TagRepository tagRepository) {
    return new AddTagViewModel(tagRepository);
  }
}
