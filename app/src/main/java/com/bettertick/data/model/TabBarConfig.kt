package com.bettertick.data.model

/**
 * User-customizable bottom nav configuration. [enabledIds] is the ordered
 * list of tab identifiers the user has kept in their bottom bar; anything
 * not in this list is "사용 안 함". [maxTabs] caps how many actually render
 * — overflowing entries fold into the "더보기" slot at runtime.
 *
 * Stored as a single doc at users/{uid}/settings/tabbar. Tab ids are free
 * strings so new tabs can be added without a schema migration; consumers
 * are responsible for ignoring ids they don't know.
 */
data class TabBarConfig(
    val enabledIds: List<String> = defaultTabBarConfig.enabledIds,
    val maxTabs: Int = 5
)

/** First-run defaults — matches the pre-customization tab bar shape so a
 *  user who never touches this setting sees the same layout as before. */
val defaultTabBarConfig = TabBarConfig(
    enabledIds = listOf("tasks", "calendar", "eisenhower", "pomodoro", "more"),
    maxTabs = 5
)
