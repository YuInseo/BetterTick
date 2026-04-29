package com.bettertick.ui.screens.focus;

import com.bettertick.data.repository.FocusRepository;
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
public final class FocusViewModel_Factory implements Factory<FocusViewModel> {
  private final Provider<FocusRepository> focusRepositoryProvider;

  public FocusViewModel_Factory(Provider<FocusRepository> focusRepositoryProvider) {
    this.focusRepositoryProvider = focusRepositoryProvider;
  }

  @Override
  public FocusViewModel get() {
    return newInstance(focusRepositoryProvider.get());
  }

  public static FocusViewModel_Factory create(Provider<FocusRepository> focusRepositoryProvider) {
    return new FocusViewModel_Factory(focusRepositoryProvider);
  }

  public static FocusViewModel newInstance(FocusRepository focusRepository) {
    return new FocusViewModel(focusRepository);
  }
}
