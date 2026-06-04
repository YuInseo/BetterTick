package com.bettertick.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bettertick.ui.screens.auth.AuthViewModel
import com.bettertick.ui.screens.auth.LoginScreen
import com.bettertick.ui.screens.auth.RegisterScreen
import com.bettertick.ui.screens.calendar.CalendarScreen
import com.bettertick.ui.screens.diary.DiaryScreen
import com.bettertick.ui.screens.focus.FocusScreen
import com.bettertick.ui.screens.focus.FocusStatsScreen
import com.bettertick.ui.screens.habits.HabitsScreen
import com.bettertick.ui.screens.more.AccountScreen
import com.bettertick.ui.screens.more.AppearanceScreen
import com.bettertick.ui.screens.more.TabBarViewModel
import com.bettertick.ui.screens.more.tabCatalog
import com.bettertick.ui.screens.lists.AddListScreen
import com.bettertick.ui.screens.matrix.MatrixEditScreen
import com.bettertick.ui.screens.matrix.MatrixQuickAddSheet
import com.bettertick.ui.screens.matrix.MatrixScreen
import com.bettertick.ui.screens.matrix.QuadrantEditScreen
import com.bettertick.ui.screens.more.MoreScreen
import com.bettertick.ui.screens.tags.AddTagScreen
import com.bettertick.ui.screens.tags.TagManagementScreen
import com.bettertick.ui.screens.more.TabBarScreen
import com.bettertick.ui.screens.more.WidgetGalleryScreen
import com.bettertick.ui.screens.tasks.QuickAddViewModel
import com.bettertick.ui.screens.tasks.TaskFilter
import com.bettertick.ui.screens.tasks.TasksScreen
import com.bettertick.ui.screens.tasks.TasksViewModel
import com.bettertick.ui.screens.tasks.components.TaskInputSheet
import com.bettertick.ui.theme.DarkBackground
import com.bettertick.ui.theme.DarkSurface
import com.bettertick.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/** Map a tab-catalog id (as stored in the user's [TabBarConfig]) to the
 *  nav route it should drive. Ids without a known route yet (dday/search)
 *  return null so the bottom nav can drop them silently. */
internal fun tabRouteFor(tabId: String): String? = when (tabId) {
    "tasks" -> BottomNavItem.Tasks.route
    "calendar" -> BottomNavItem.Calendar.route
    "eisenhower" -> BottomNavItem.Matrix.route
    "pomodoro" -> BottomNavItem.Focus.route
    "habits" -> BottomNavItem.Habits.route
    "diary" -> BottomNavItem.Diary.route
    "more" -> BottomNavItem.More.route
    else -> null
}

/** Jump to the Tasks tab without re-adding it to the backstack. Used from
 *  the drawer so selecting a list always lands on the list view, even when
 *  the user is currently on Calendar/Focus/etc. */
private fun jumpToTasksTab(navController: androidx.navigation.NavHostController) {
    navController.navigate(BottomNavItem.Tasks.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun BetterTickNavHost(openQuickAdd: MutableState<Boolean> = remember { mutableStateOf(false) }) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    when (isLoggedIn) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            )
        }
        false -> {
            AuthContent(
                onLoginSuccess = { authViewModel.refreshAuthState() },
                onRegisterSuccess = { authViewModel.refreshAuthState() }
            )
        }
        true -> {
            MainContent(authViewModel = authViewModel, openQuickAdd = openQuickAdd)
        }
    }
}

@Composable
private fun AuthContent(
    onLoginSuccess: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = onLoginSuccess
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = onRegisterSuccess
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    authViewModel: AuthViewModel,
    openQuickAdd: MutableState<Boolean>
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val quickAddViewModel: QuickAddViewModel = hiltViewModel()
    val tasksViewModel: TasksViewModel = hiltViewModel()
    val allTasks by tasksViewModel.allTasks.collectAsState()
    val lists by tasksViewModel.lists.collectAsState()
    val tags by tasksViewModel.tags.collectAsState()
    val currentFilter by tasksViewModel.currentFilter.collectAsState()
    val tabBarViewModel: TabBarViewModel = hiltViewModel()
    val tabBarConfig by tabBarViewModel.config.collectAsState()
    var showTaskInput by remember { mutableStateOf(false) }
    var showOverflowSheet by remember { mutableStateOf(false) }
    var calendarSelectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    LaunchedEffect(openQuickAdd.value) {
        if (openQuickAdd.value) {
            showTaskInput = true
            openQuickAdd.value = false
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTabRoute = currentRoute == null || BottomNavItem.items.any { it.route == currentRoute }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkBackground
            ) {
                val today = java.time.LocalDate.now()
                val todayCount = remember(allTasks, today) {
                    allTasks.count { t ->
                        !t.isCompleted && !t.isAbandoned &&
                            t.dueDate?.toDate()?.toInstant()
                                ?.atZone(java.time.ZoneId.systemDefault())
                                ?.toLocalDate() == today
                    }
                }
                val inboxCount = remember(allTasks) {
                    allTasks.count { !it.isCompleted && !it.isAbandoned }
                }
                val taskCountByList = remember(allTasks) {
                    allTasks.asSequence()
                        .filter { !it.isCompleted && !it.isAbandoned }
                        .groupingBy { it.listId }
                        .eachCount()
                }
                val taskCountByTag = remember(allTasks) {
                    val counts = mutableMapOf<String, Int>()
                    allTasks.forEach { task ->
                        if (task.isCompleted || task.isAbandoned) return@forEach
                        task.tagIds.forEach { id ->
                            counts[id] = (counts[id] ?: 0) + 1
                        }
                    }
                    counts
                }
                val selectedFilterId = when (val f = currentFilter) {
                    is TaskFilter.Today -> "today"
                    is TaskFilter.Inbox -> "inbox"
                    is TaskFilter.ByList -> f.listId
                }
                DrawerContent(
                    userName = authViewModel.userName,
                    userPhotoUrl = authViewModel.userPhotoUrl,
                    todayCount = todayCount,
                    inboxCount = inboxCount,
                    lists = lists,
                    tags = tags,
                    taskCountByList = taskCountByList,
                    taskCountByTag = taskCountByTag,
                    selectedFilter = selectedFilterId,
                    onTodayClick = {
                        tasksViewModel.setFilter(TaskFilter.Today)
                        jumpToTasksTab(navController)
                        scope.launch { drawerState.close() }
                    },
                    onInboxClick = {
                        tasksViewModel.setFilter(TaskFilter.Inbox)
                        jumpToTasksTab(navController)
                        scope.launch { drawerState.close() }
                    },
                    onListClick = { list ->
                        tasksViewModel.setFilter(TaskFilter.ByList(list.id, list.name))
                        jumpToTasksTab(navController)
                        scope.launch { drawerState.close() }
                    },
                    onTagClick = { _ ->
                        scope.launch { drawerState.close() }
                    },
                    onAddListClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("addlist")
                    },
                    onAddTagClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("addtag")
                    },
                    onEditTagsClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("tags")
                    },
                    onEditListClick = { list ->
                        scope.launch { drawerState.close() }
                        navController.navigate("editlist/${list.id}")
                    },
                    onTogglePin = { list ->
                        tasksViewModel.togglePinned(list.id, !list.isPinned)
                    },
                    onDeleteList = { list ->
                        tasksViewModel.deleteList(list.id)
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = DarkBackground,
            floatingActionButton = {
                if (isTabRoute &&
                    currentRoute != BottomNavItem.Habits.route &&
                    currentRoute != BottomNavItem.Diary.route
                ) {
                    FloatingActionButton(
                        onClick = { showTaskInput = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add task",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            bottomBar = {
                if (isTabRoute) {
                    val accent = MaterialTheme.colorScheme.primary
                    val catalog = remember { tabCatalog() }
                    val allUserItems = remember(tabBarConfig, catalog) {
                        tabBarConfig.enabledIds
                            .filter { it != "more" }
                            .mapNotNull { id ->
                                val routed = tabRouteFor(id) ?: return@mapNotNull null
                                val tab = catalog.firstOrNull { it.id == id }
                                    ?: return@mapNotNull null
                                Triple(id, tab, routed)
                            }
                    }
                    val userCap = (tabBarConfig.maxTabs - 1).coerceAtLeast(0)
                    val visibleUserItems = allUserItems.take(userCap)
                    val overflowItems = allUserItems.drop(userCap)
                    val hasOverflow = overflowItems.isNotEmpty()

                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = accent
                    ) {
                        val currentDestination = navBackStackEntry?.destination

                        visibleUserItems.forEach { (_, tab, route) ->
                            val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.name
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = accent,
                                    selectedTextColor = accent,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }

                        val moreTab = catalog.firstOrNull { it.id == "more" }
                        val moreRoute = BottomNavItem.More.route
                        if (hasOverflow) {
                            val overflowOrMoreSelected = overflowItems.any { (_, _, r) ->
                                currentDestination?.hierarchy?.any { it.route == r } == true
                            } || currentDestination?.hierarchy?.any { it.route == moreRoute } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreHoriz,
                                        contentDescription = "더보기"
                                    )
                                },
                                selected = overflowOrMoreSelected,
                                onClick = { showOverflowSheet = true },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = accent,
                                    selectedTextColor = accent,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else if (moreTab != null) {
                            val selected = currentDestination?.hierarchy?.any { it.route == moreRoute } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = moreTab.icon,
                                        contentDescription = moreTab.name
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(moreRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = accent,
                                    selectedTextColor = accent,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Tasks.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(BottomNavItem.Tasks.route) {
                    TasksScreen(
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
                        viewModel = tasksViewModel
                    )
                }
                composable(BottomNavItem.Calendar.route) {
                    CalendarScreen(
                        onSelectedDateChanged = { calendarSelectedDate = it }
                    )
                }
                composable(BottomNavItem.Matrix.route) {
                    MatrixScreen(
                        onEdit = { navController.navigate("matrixedit") }
                    )
                }
                composable("matrixedit") {
                    MatrixEditScreen(
                        onBack = { navController.popBackStack() },
                        onEditQuadrant = { id -> navController.navigate("quadrantedit/$id") }
                    )
                }
                composable(
                    route = "quadrantedit/{quadrantId}",
                    arguments = listOf(androidx.navigation.navArgument("quadrantId") {
                        type = androidx.navigation.NavType.StringType
                    })
                ) { entry ->
                    val id = entry.arguments?.getString("quadrantId") ?: "I"
                    QuadrantEditScreen(
                        quadrantId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(BottomNavItem.Focus.route) {
                    FocusScreen(onOpenStats = { navController.navigate("focusstats") })
                }
                composable("focusstats") {
                    FocusStatsScreen(onBack = { navController.popBackStack() })
                }
                composable(BottomNavItem.Habits.route) {
                    HabitsScreen()
                }
                composable(BottomNavItem.Diary.route) {
                    DiaryScreen()
                }
                composable(BottomNavItem.More.route) {
                    MoreScreen(
                        userName = authViewModel.userName,
                        userPhotoUrl = authViewModel.userPhotoUrl,
                        onSignOut = { authViewModel.signOut() },
                        onNavigateToAppearance = { navController.navigate("appearance") },
                        onNavigateToTabBar = { navController.navigate("tabbar") },
                        onNavigateToWidgets = { navController.navigate("widgets") },
                        onNavigateToAccount = { navController.navigate("account") }
                    )
                }
                composable("appearance") {
                    AppearanceScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("tabbar") {
                    TabBarScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("widgets") {
                    WidgetGalleryScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("account") {
                    AccountScreen(
                        displayName = authViewModel.userDisplayName,
                        email = authViewModel.userEmail,
                        photoUrl = authViewModel.userPhotoUrl,
                        isGoogleUser = authViewModel.isGoogleUser,
                        onBack = { navController.popBackStack() },
                        onDeleteAccount = { /* TODO: implement account deletion */ }
                    )
                }
                composable("addlist") {
                    AddListScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "editlist/{listId}",
                    arguments = listOf(androidx.navigation.navArgument("listId") {
                        type = androidx.navigation.NavType.StringType
                    })
                ) { entry ->
                    val listId = entry.arguments?.getString("listId")
                    val target = lists.firstOrNull { it.id == listId }
                    AddListScreen(
                        onBack = { navController.popBackStack() },
                        initialList = target,
                        onDelete = {
                            if (target != null) tasksViewModel.deleteList(target.id)
                            navController.popBackStack()
                        }
                    )
                }
                composable("addtag") {
                    AddTagScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "edittag/{tagId}",
                    arguments = listOf(androidx.navigation.navArgument("tagId") {
                        type = androidx.navigation.NavType.StringType
                    })
                ) { entry ->
                    val tagId = entry.arguments?.getString("tagId")
                    val target = tags.firstOrNull { it.id == tagId }
                    AddTagScreen(
                        onBack = { navController.popBackStack() },
                        initialTag = target
                    )
                }
                composable("tags") {
                    TagManagementScreen(
                        onBack = { navController.popBackStack() },
                        onAddTag = { navController.navigate("addtag") },
                        onEditTag = { id -> navController.navigate("edittag/$id") }
                    )
                }
            }
        }
    }

    if (showOverflowSheet) {
        val catalog = remember { tabCatalog() }
        val allUserItems = remember(tabBarConfig, catalog) {
            tabBarConfig.enabledIds
                .filter { it != "more" }
                .mapNotNull { id ->
                    val routed = tabRouteFor(id) ?: return@mapNotNull null
                    val tab = catalog.firstOrNull { it.id == id } ?: return@mapNotNull null
                    Triple(id, tab, routed)
                }
        }
        val userCap = (tabBarConfig.maxTabs - 1).coerceAtLeast(0)
        val overflowItems = allUserItems.drop(userCap)
        val moreTab = catalog.firstOrNull { it.id == "more" }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showOverflowSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "더보기",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                val entries = overflowItems + listOfNotNull(
                    moreTab?.let { Triple("more", it, BottomNavItem.More.route) }
                )
                entries.forEach { (_, tab, route) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showOverflowSheet = false
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.name,
                            tint = TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = tab.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTaskInput) {
        Dialog(
            onDismissRequest = { showTaskInput = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    if (currentRoute == BottomNavItem.Matrix.route) {
                        MatrixQuickAddSheet(
                            onDismiss = { showTaskInput = false }
                        )
                    } else {
                        val selectedKanbanColumn by tasksViewModel.selectedKanbanColumn.collectAsState()
                        val contextListId = (currentFilter as? TaskFilter.ByList)?.listId ?: ""
                        val isKanbanContext = contextListId.isNotEmpty() &&
                            lists.firstOrNull { it.id == contextListId }?.viewType == "kanban"
                        val contextColumn = if (isKanbanContext) selectedKanbanColumn else ""
                        TaskInputSheet(
                            onAddTask = { title, date ->
                                quickAddViewModel.addTask(
                                    title = title,
                                    date = if (isKanbanContext) null else date,
                                    listId = contextListId,
                                    kanbanColumn = contextColumn
                                )
                                showTaskInput = false
                            },
                            onDismiss = { showTaskInput = false },
                            initialDate = if (currentRoute == BottomNavItem.Calendar.route)
                                calendarSelectedDate ?: java.time.LocalDate.now()
                            else
                                java.time.LocalDate.now()
                        )
                    }
                }
            }
        }
    }
}
