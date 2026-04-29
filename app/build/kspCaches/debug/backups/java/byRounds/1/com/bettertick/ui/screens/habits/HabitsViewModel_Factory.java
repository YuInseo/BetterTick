package com.bettertick.ui.screens.habits;

import com.bettertick.data.repository.HabitRepository;
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
public final class HabitsViewModel_Factory implements Factory<HabitsViewModel> {
  private final Provider<HabitRepository> habitRepositoryProvider;

  public HabitsViewModel_Factory(Provider<HabitRepository> habitRepositoryProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
  }

  @Override
  public HabitsViewModel get() {
    return newInstance(habitRepositoryProvider.get());
  }

  public static HabitsViewModel_Factory create(Provider<HabitRepository> habitRepositoryProvider) {
    return new HabitsViewModel_Factory(habitRepositoryProvider);
  }

  public static HabitsViewModel newInstance(HabitRepository habitRepository) {
    return new HabitsViewModel(habitRepository);
  }
}
