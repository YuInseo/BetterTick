package com.bettertick;

import com.bettertick.data.repository.AuthRepository;
import com.bettertick.data.repository.FocusRepository;
import com.bettertick.data.repository.HabitRepository;
import com.bettertick.data.repository.ListRepository;
import com.bettertick.data.repository.TaskRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BetterTickApplication_MembersInjector implements MembersInjector<BetterTickApplication> {
  private final Provider<TaskRepository> taskRepositoryProvider;

  private final Provider<HabitRepository> habitRepositoryProvider;

  private final Provider<FocusRepository> focusRepositoryProvider;

  private final Provider<ListRepository> listRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public BetterTickApplication_MembersInjector(Provider<TaskRepository> taskRepositoryProvider,
      Provider<HabitRepository> habitRepositoryProvider,
      Provider<FocusRepository> focusRepositoryProvider,
      Provider<ListRepository> listRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.taskRepositoryProvider = taskRepositoryProvider;
    this.habitRepositoryProvider = habitRepositoryProvider;
    this.focusRepositoryProvider = focusRepositoryProvider;
    this.listRepositoryProvider = listRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  public static MembersInjector<BetterTickApplication> create(
      Provider<TaskRepository> taskRepositoryProvider,
      Provider<HabitRepository> habitRepositoryProvider,
      Provider<FocusRepository> focusRepositoryProvider,
      Provider<ListRepository> listRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new BetterTickApplication_MembersInjector(taskRepositoryProvider, habitRepositoryProvider, focusRepositoryProvider, listRepositoryProvider, authRepositoryProvider);
  }

  @Override
  public void injectMembers(BetterTickApplication instance) {
    injectTaskRepository(instance, taskRepositoryProvider.get());
    injectHabitRepository(instance, habitRepositoryProvider.get());
    injectFocusRepository(instance, focusRepositoryProvider.get());
    injectListRepository(instance, listRepositoryProvider.get());
    injectAuthRepository(instance, authRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.bettertick.BetterTickApplication.taskRepository")
  public static void injectTaskRepository(BetterTickApplication instance,
      TaskRepository taskRepository) {
    instance.taskRepository = taskRepository;
  }

  @InjectedFieldSignature("com.bettertick.BetterTickApplication.habitRepository")
  public static void injectHabitRepository(BetterTickApplication instance,
      HabitRepository habitRepository) {
    instance.habitRepository = habitRepository;
  }

  @InjectedFieldSignature("com.bettertick.BetterTickApplication.focusRepository")
  public static void injectFocusRepository(BetterTickApplication instance,
      FocusRepository focusRepository) {
    instance.focusRepository = focusRepository;
  }

  @InjectedFieldSignature("com.bettertick.BetterTickApplication.listRepository")
  public static void injectListRepository(BetterTickApplication instance,
      ListRepository listRepository) {
    instance.listRepository = listRepository;
  }

  @InjectedFieldSignature("com.bettertick.BetterTickApplication.authRepository")
  public static void injectAuthRepository(BetterTickApplication instance,
      AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }
}
