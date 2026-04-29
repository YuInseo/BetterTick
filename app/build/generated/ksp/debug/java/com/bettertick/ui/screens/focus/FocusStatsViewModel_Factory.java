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
public final class FocusStatsViewModel_Factory implements Factory<FocusStatsViewModel> {
  private final Provider<FocusRepository> focusRepositoryProvider;

  public FocusStatsViewModel_Factory(Provider<FocusRepository> focusRepositoryProvider) {
    this.focusRepositoryProvider = focusRepositoryProvider;
  }

  @Override
  public FocusStatsViewModel get() {
    return newInstance(focusRepositoryProvider.get());
  }

  public static FocusStatsViewModel_Factory create(
      Provider<FocusRepository> focusRepositoryProvider) {
    return new FocusStatsViewModel_Factory(focusRepositoryProvider);
  }

  public static FocusStatsViewModel newInstance(FocusRepository focusRepository) {
    return new FocusStatsViewModel(focusRepository);
  }
}
