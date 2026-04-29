package com.bettertick;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.bettertick.data.firebase.DataSeeder;
import com.bettertick.data.firebase.FirestoreProvider;
import com.bettertick.data.repository.AuthRepository;
import com.bettertick.data.repository.FocusRepository;
import com.bettertick.data.repository.HabitRepository;
import com.bettertick.data.repository.ListRepository;
import com.bettertick.data.repository.MatrixRepository;
import com.bettertick.data.repository.TabBarRepository;
import com.bettertick.data.repository.TagRepository;
import com.bettertick.data.repository.TaskRepository;
import com.bettertick.di.AppModule_ProvideAuthRepositoryFactory;
import com.bettertick.di.AppModule_ProvideDataSeederFactory;
import com.bettertick.di.AppModule_ProvideFirebaseAuthFactory;
import com.bettertick.di.AppModule_ProvideFirebaseFirestoreFactory;
import com.bettertick.di.AppModule_ProvideFirestoreProviderFactory;
import com.bettertick.di.AppModule_ProvideFocusRepositoryFactory;
import com.bettertick.di.AppModule_ProvideHabitRepositoryFactory;
import com.bettertick.di.AppModule_ProvideListRepositoryFactory;
import com.bettertick.di.AppModule_ProvideMatrixRepositoryFactory;
import com.bettertick.di.AppModule_ProvideTabBarRepositoryFactory;
import com.bettertick.di.AppModule_ProvideTagRepositoryFactory;
import com.bettertick.di.AppModule_ProvideTaskRepositoryFactory;
import com.bettertick.ui.screens.auth.AuthViewModel;
import com.bettertick.ui.screens.auth.AuthViewModel_HiltModules;
import com.bettertick.ui.screens.calendar.CalendarViewModel;
import com.bettertick.ui.screens.calendar.CalendarViewModel_HiltModules;
import com.bettertick.ui.screens.focus.FocusStatsViewModel;
import com.bettertick.ui.screens.focus.FocusStatsViewModel_HiltModules;
import com.bettertick.ui.screens.focus.FocusViewModel;
import com.bettertick.ui.screens.focus.FocusViewModel_HiltModules;
import com.bettertick.ui.screens.habits.HabitsViewModel;
import com.bettertick.ui.screens.habits.HabitsViewModel_HiltModules;
import com.bettertick.ui.screens.lists.AddListViewModel;
import com.bettertick.ui.screens.lists.AddListViewModel_HiltModules;
import com.bettertick.ui.screens.matrix.MatrixViewModel;
import com.bettertick.ui.screens.matrix.MatrixViewModel_HiltModules;
import com.bettertick.ui.screens.more.TabBarViewModel;
import com.bettertick.ui.screens.more.TabBarViewModel_HiltModules;
import com.bettertick.ui.screens.tags.AddTagViewModel;
import com.bettertick.ui.screens.tags.AddTagViewModel_HiltModules;
import com.bettertick.ui.screens.tasks.QuickAddViewModel;
import com.bettertick.ui.screens.tasks.QuickAddViewModel_HiltModules;
import com.bettertick.ui.screens.tasks.TasksViewModel;
import com.bettertick.ui.screens.tasks.TasksViewModel_HiltModules;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerBetterTickApplication_HiltComponents_SingletonC {
  private DaggerBetterTickApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public BetterTickApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements BetterTickApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements BetterTickApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements BetterTickApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements BetterTickApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements BetterTickApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements BetterTickApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements BetterTickApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public BetterTickApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends BetterTickApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends BetterTickApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends BetterTickApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends BetterTickApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
    }

    @Override
    public void injectQuickAddActivity(QuickAddActivity arg0) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(11).put(LazyClassKeyProvider.com_bettertick_ui_screens_lists_AddListViewModel, AddListViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_tags_AddTagViewModel, AddTagViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_auth_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_calendar_CalendarViewModel, CalendarViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_focus_FocusStatsViewModel, FocusStatsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_focus_FocusViewModel, FocusViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_habits_HabitsViewModel, HabitsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_matrix_MatrixViewModel, MatrixViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_tasks_QuickAddViewModel, QuickAddViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_more_TabBarViewModel, TabBarViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_bettertick_ui_screens_tasks_TasksViewModel, TasksViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_bettertick_ui_screens_focus_FocusStatsViewModel = "com.bettertick.ui.screens.focus.FocusStatsViewModel";

      static String com_bettertick_ui_screens_habits_HabitsViewModel = "com.bettertick.ui.screens.habits.HabitsViewModel";

      static String com_bettertick_ui_screens_lists_AddListViewModel = "com.bettertick.ui.screens.lists.AddListViewModel";

      static String com_bettertick_ui_screens_tags_AddTagViewModel = "com.bettertick.ui.screens.tags.AddTagViewModel";

      static String com_bettertick_ui_screens_tasks_TasksViewModel = "com.bettertick.ui.screens.tasks.TasksViewModel";

      static String com_bettertick_ui_screens_auth_AuthViewModel = "com.bettertick.ui.screens.auth.AuthViewModel";

      static String com_bettertick_ui_screens_calendar_CalendarViewModel = "com.bettertick.ui.screens.calendar.CalendarViewModel";

      static String com_bettertick_ui_screens_tasks_QuickAddViewModel = "com.bettertick.ui.screens.tasks.QuickAddViewModel";

      static String com_bettertick_ui_screens_more_TabBarViewModel = "com.bettertick.ui.screens.more.TabBarViewModel";

      static String com_bettertick_ui_screens_matrix_MatrixViewModel = "com.bettertick.ui.screens.matrix.MatrixViewModel";

      static String com_bettertick_ui_screens_focus_FocusViewModel = "com.bettertick.ui.screens.focus.FocusViewModel";

      @KeepFieldType
      FocusStatsViewModel com_bettertick_ui_screens_focus_FocusStatsViewModel2;

      @KeepFieldType
      HabitsViewModel com_bettertick_ui_screens_habits_HabitsViewModel2;

      @KeepFieldType
      AddListViewModel com_bettertick_ui_screens_lists_AddListViewModel2;

      @KeepFieldType
      AddTagViewModel com_bettertick_ui_screens_tags_AddTagViewModel2;

      @KeepFieldType
      TasksViewModel com_bettertick_ui_screens_tasks_TasksViewModel2;

      @KeepFieldType
      AuthViewModel com_bettertick_ui_screens_auth_AuthViewModel2;

      @KeepFieldType
      CalendarViewModel com_bettertick_ui_screens_calendar_CalendarViewModel2;

      @KeepFieldType
      QuickAddViewModel com_bettertick_ui_screens_tasks_QuickAddViewModel2;

      @KeepFieldType
      TabBarViewModel com_bettertick_ui_screens_more_TabBarViewModel2;

      @KeepFieldType
      MatrixViewModel com_bettertick_ui_screens_matrix_MatrixViewModel2;

      @KeepFieldType
      FocusViewModel com_bettertick_ui_screens_focus_FocusViewModel2;
    }
  }

  private static final class ViewModelCImpl extends BetterTickApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AddListViewModel> addListViewModelProvider;

    private Provider<AddTagViewModel> addTagViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<CalendarViewModel> calendarViewModelProvider;

    private Provider<FocusStatsViewModel> focusStatsViewModelProvider;

    private Provider<FocusViewModel> focusViewModelProvider;

    private Provider<HabitsViewModel> habitsViewModelProvider;

    private Provider<MatrixViewModel> matrixViewModelProvider;

    private Provider<QuickAddViewModel> quickAddViewModelProvider;

    private Provider<TabBarViewModel> tabBarViewModelProvider;

    private Provider<TasksViewModel> tasksViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.addListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.addTagViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.calendarViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.focusStatsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.focusViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.habitsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.matrixViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.quickAddViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.tabBarViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.tasksViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(11).put(LazyClassKeyProvider.com_bettertick_ui_screens_lists_AddListViewModel, ((Provider) addListViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_tags_AddTagViewModel, ((Provider) addTagViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_auth_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_calendar_CalendarViewModel, ((Provider) calendarViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_focus_FocusStatsViewModel, ((Provider) focusStatsViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_focus_FocusViewModel, ((Provider) focusViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_habits_HabitsViewModel, ((Provider) habitsViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_matrix_MatrixViewModel, ((Provider) matrixViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_tasks_QuickAddViewModel, ((Provider) quickAddViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_more_TabBarViewModel, ((Provider) tabBarViewModelProvider)).put(LazyClassKeyProvider.com_bettertick_ui_screens_tasks_TasksViewModel, ((Provider) tasksViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_bettertick_ui_screens_tags_AddTagViewModel = "com.bettertick.ui.screens.tags.AddTagViewModel";

      static String com_bettertick_ui_screens_tasks_QuickAddViewModel = "com.bettertick.ui.screens.tasks.QuickAddViewModel";

      static String com_bettertick_ui_screens_tasks_TasksViewModel = "com.bettertick.ui.screens.tasks.TasksViewModel";

      static String com_bettertick_ui_screens_matrix_MatrixViewModel = "com.bettertick.ui.screens.matrix.MatrixViewModel";

      static String com_bettertick_ui_screens_habits_HabitsViewModel = "com.bettertick.ui.screens.habits.HabitsViewModel";

      static String com_bettertick_ui_screens_focus_FocusViewModel = "com.bettertick.ui.screens.focus.FocusViewModel";

      static String com_bettertick_ui_screens_more_TabBarViewModel = "com.bettertick.ui.screens.more.TabBarViewModel";

      static String com_bettertick_ui_screens_lists_AddListViewModel = "com.bettertick.ui.screens.lists.AddListViewModel";

      static String com_bettertick_ui_screens_calendar_CalendarViewModel = "com.bettertick.ui.screens.calendar.CalendarViewModel";

      static String com_bettertick_ui_screens_auth_AuthViewModel = "com.bettertick.ui.screens.auth.AuthViewModel";

      static String com_bettertick_ui_screens_focus_FocusStatsViewModel = "com.bettertick.ui.screens.focus.FocusStatsViewModel";

      @KeepFieldType
      AddTagViewModel com_bettertick_ui_screens_tags_AddTagViewModel2;

      @KeepFieldType
      QuickAddViewModel com_bettertick_ui_screens_tasks_QuickAddViewModel2;

      @KeepFieldType
      TasksViewModel com_bettertick_ui_screens_tasks_TasksViewModel2;

      @KeepFieldType
      MatrixViewModel com_bettertick_ui_screens_matrix_MatrixViewModel2;

      @KeepFieldType
      HabitsViewModel com_bettertick_ui_screens_habits_HabitsViewModel2;

      @KeepFieldType
      FocusViewModel com_bettertick_ui_screens_focus_FocusViewModel2;

      @KeepFieldType
      TabBarViewModel com_bettertick_ui_screens_more_TabBarViewModel2;

      @KeepFieldType
      AddListViewModel com_bettertick_ui_screens_lists_AddListViewModel2;

      @KeepFieldType
      CalendarViewModel com_bettertick_ui_screens_calendar_CalendarViewModel2;

      @KeepFieldType
      AuthViewModel com_bettertick_ui_screens_auth_AuthViewModel2;

      @KeepFieldType
      FocusStatsViewModel com_bettertick_ui_screens_focus_FocusStatsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.bettertick.ui.screens.lists.AddListViewModel 
          return (T) new AddListViewModel(singletonCImpl.provideListRepositoryProvider.get());

          case 1: // com.bettertick.ui.screens.tags.AddTagViewModel 
          return (T) new AddTagViewModel(singletonCImpl.provideTagRepositoryProvider.get());

          case 2: // com.bettertick.ui.screens.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 3: // com.bettertick.ui.screens.calendar.CalendarViewModel 
          return (T) new CalendarViewModel(singletonCImpl.provideTaskRepositoryProvider.get(), singletonCImpl.provideListRepositoryProvider.get(), singletonCImpl.provideTagRepositoryProvider.get());

          case 4: // com.bettertick.ui.screens.focus.FocusStatsViewModel 
          return (T) new FocusStatsViewModel(singletonCImpl.provideFocusRepositoryProvider.get());

          case 5: // com.bettertick.ui.screens.focus.FocusViewModel 
          return (T) new FocusViewModel(singletonCImpl.provideFocusRepositoryProvider.get());

          case 6: // com.bettertick.ui.screens.habits.HabitsViewModel 
          return (T) new HabitsViewModel(singletonCImpl.provideHabitRepositoryProvider.get());

          case 7: // com.bettertick.ui.screens.matrix.MatrixViewModel 
          return (T) new MatrixViewModel(singletonCImpl.provideMatrixRepositoryProvider.get(), singletonCImpl.provideTaskRepositoryProvider.get(), singletonCImpl.provideListRepositoryProvider.get(), singletonCImpl.provideTagRepositoryProvider.get());

          case 8: // com.bettertick.ui.screens.tasks.QuickAddViewModel 
          return (T) new QuickAddViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideTaskRepositoryProvider.get());

          case 9: // com.bettertick.ui.screens.more.TabBarViewModel 
          return (T) new TabBarViewModel(singletonCImpl.provideTabBarRepositoryProvider.get());

          case 10: // com.bettertick.ui.screens.tasks.TasksViewModel 
          return (T) new TasksViewModel(singletonCImpl.provideTaskRepositoryProvider.get(), singletonCImpl.provideListRepositoryProvider.get(), singletonCImpl.provideTagRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends BetterTickApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends BetterTickApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends BetterTickApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<FirebaseAuth> provideFirebaseAuthProvider;

    private Provider<FirebaseFirestore> provideFirebaseFirestoreProvider;

    private Provider<FirestoreProvider> provideFirestoreProvider;

    private Provider<TaskRepository> provideTaskRepositoryProvider;

    private Provider<HabitRepository> provideHabitRepositoryProvider;

    private Provider<FocusRepository> provideFocusRepositoryProvider;

    private Provider<ListRepository> provideListRepositoryProvider;

    private Provider<DataSeeder> provideDataSeederProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<TagRepository> provideTagRepositoryProvider;

    private Provider<MatrixRepository> provideMatrixRepositoryProvider;

    private Provider<TabBarRepository> provideTabBarRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 2));
      this.provideFirebaseFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 3));
      this.provideFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirestoreProvider>(singletonCImpl, 1));
      this.provideTaskRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<TaskRepository>(singletonCImpl, 0));
      this.provideHabitRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<HabitRepository>(singletonCImpl, 4));
      this.provideFocusRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<FocusRepository>(singletonCImpl, 5));
      this.provideListRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ListRepository>(singletonCImpl, 6));
      this.provideDataSeederProvider = DoubleCheck.provider(new SwitchingProvider<DataSeeder>(singletonCImpl, 8));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 7));
      this.provideTagRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<TagRepository>(singletonCImpl, 9));
      this.provideMatrixRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<MatrixRepository>(singletonCImpl, 10));
      this.provideTabBarRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<TabBarRepository>(singletonCImpl, 11));
    }

    @Override
    public void injectBetterTickApplication(BetterTickApplication betterTickApplication) {
      injectBetterTickApplication2(betterTickApplication);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private BetterTickApplication injectBetterTickApplication2(BetterTickApplication instance) {
      BetterTickApplication_MembersInjector.injectTaskRepository(instance, provideTaskRepositoryProvider.get());
      BetterTickApplication_MembersInjector.injectHabitRepository(instance, provideHabitRepositoryProvider.get());
      BetterTickApplication_MembersInjector.injectFocusRepository(instance, provideFocusRepositoryProvider.get());
      BetterTickApplication_MembersInjector.injectListRepository(instance, provideListRepositoryProvider.get());
      BetterTickApplication_MembersInjector.injectAuthRepository(instance, provideAuthRepositoryProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.bettertick.data.repository.TaskRepository 
          return (T) AppModule_ProvideTaskRepositoryFactory.provideTaskRepository(singletonCImpl.provideFirestoreProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.bettertick.data.firebase.FirestoreProvider 
          return (T) AppModule_ProvideFirestoreProviderFactory.provideFirestoreProvider(singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideFirebaseFirestoreProvider.get());

          case 2: // com.google.firebase.auth.FirebaseAuth 
          return (T) AppModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 3: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) AppModule_ProvideFirebaseFirestoreFactory.provideFirebaseFirestore();

          case 4: // com.bettertick.data.repository.HabitRepository 
          return (T) AppModule_ProvideHabitRepositoryFactory.provideHabitRepository(singletonCImpl.provideFirestoreProvider.get());

          case 5: // com.bettertick.data.repository.FocusRepository 
          return (T) AppModule_ProvideFocusRepositoryFactory.provideFocusRepository(singletonCImpl.provideFirestoreProvider.get());

          case 6: // com.bettertick.data.repository.ListRepository 
          return (T) AppModule_ProvideListRepositoryFactory.provideListRepository(singletonCImpl.provideFirestoreProvider.get());

          case 7: // com.bettertick.data.repository.AuthRepository 
          return (T) AppModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideFirebaseAuthProvider.get(), singletonCImpl.provideFirestoreProvider.get(), singletonCImpl.provideDataSeederProvider.get());

          case 8: // com.bettertick.data.firebase.DataSeeder 
          return (T) AppModule_ProvideDataSeederFactory.provideDataSeeder(singletonCImpl.provideFirestoreProvider.get());

          case 9: // com.bettertick.data.repository.TagRepository 
          return (T) AppModule_ProvideTagRepositoryFactory.provideTagRepository(singletonCImpl.provideFirestoreProvider.get());

          case 10: // com.bettertick.data.repository.MatrixRepository 
          return (T) AppModule_ProvideMatrixRepositoryFactory.provideMatrixRepository(singletonCImpl.provideFirestoreProvider.get());

          case 11: // com.bettertick.data.repository.TabBarRepository 
          return (T) AppModule_ProvideTabBarRepositoryFactory.provideTabBarRepository(singletonCImpl.provideFirestoreProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
