package com.bettertick.ui.screens.lists;

import com.bettertick.data.repository.ListRepository;
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
public final class AddListViewModel_Factory implements Factory<AddListViewModel> {
  private final Provider<ListRepository> listRepositoryProvider;

  public AddListViewModel_Factory(Provider<ListRepository> listRepositoryProvider) {
    this.listRepositoryProvider = listRepositoryProvider;
  }

  @Override
  public AddListViewModel get() {
    return newInstance(listRepositoryProvider.get());
  }

  public static AddListViewModel_Factory create(Provider<ListRepository> listRepositoryProvider) {
    return new AddListViewModel_Factory(listRepositoryProvider);
  }

  public static AddListViewModel newInstance(ListRepository listRepository) {
    return new AddListViewModel(listRepository);
  }
}
