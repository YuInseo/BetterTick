package com.bettertick.ui.screens.more;

import com.bettertick.data.repository.TabBarRepository;
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
public final class TabBarViewModel_Factory implements Factory<TabBarViewModel> {
  private final Provider<TabBarRepository> repositoryProvider;

  public TabBarViewModel_Factory(Provider<TabBarRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TabBarViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TabBarViewModel_Factory create(Provider<TabBarRepository> repositoryProvider) {
    return new TabBarViewModel_Factory(repositoryProvider);
  }

  public static TabBarViewModel newInstance(TabBarRepository repository) {
    return new TabBarViewModel(repository);
  }
}
