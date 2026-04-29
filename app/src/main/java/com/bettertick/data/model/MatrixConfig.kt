package com.bettertick.data.model

/**
 * Eisenhower matrix configuration — four quadrants, each with its own filter.
 * Persisted as a single document at users/{uid}/settings/matrix so the shape
 * stays flat and Firestore can deserialize it via its usual POJO path.
 *
 * When no config exists yet the app falls back to [defaultMatrix] which gives
 * the classic "priority per quadrant" split.
 */
data class MatrixConfig(
    val quadrants: List<QuadrantConfig> = defaultMatrix.quadrants,
    val hideCompleted: Boolean = false,
    val todayOnly: Boolean = false
)

/** Filter spec for a single quadrant. listIds/tagIds empty lists = "all". */
data class QuadrantConfig(
    val id: String = "I",
    val nameKo: String = "",
    val nameEn: String = "",
    val colorHex: String = "#FF5D5D",
    val listIds: List<String> = emptyList(),
    val tagMode: String = TagMode.Any.key,
    val tagIds: List<String> = emptyList(),
    val dateMode: String = DateMode.All.key,
    val priority: Int = -1,             // -1 = any, 0-3 = exact match
    val typeMode: String = TypeMode.All.key
) {
    enum class TagMode(val key: String, val label: String) {
        Any("any", "전체"),
        Has("has", "태그 포함"),
        Lacks("lacks", "태그 제외");
        companion object { fun from(key: String) = entries.firstOrNull { it.key == key } ?: Any }
    }

    enum class DateMode(val key: String, val label: String) {
        All("all", "전체"),
        Today("today", "오늘"),
        Overdue("overdue", "기한 지남"),
        NoDate("none", "날짜 없음");
        companion object { fun from(key: String) = entries.firstOrNull { it.key == key } ?: All }
    }

    enum class TypeMode(val key: String, val label: String) {
        All("all", "전체"),
        Task("task", "과제"),
        Note("note", "노트");
        companion object { fun from(key: String) = entries.firstOrNull { it.key == key } ?: All }
    }
}

/** Classic Eisenhower split — quadrants partition tasks by priority level so
 *  even a brand-new user sees something sensible before they touch the edit
 *  screen. */
val defaultMatrix = MatrixConfig(
    quadrants = listOf(
        QuadrantConfig(
            id = "I",
            nameKo = "긴급하고 중요한 일",
            nameEn = "Urgent & Important",
            colorHex = "#FF5D5D",
            priority = 3
        ),
        QuadrantConfig(
            id = "II",
            nameKo = "중요하지만 급하지 않은 일",
            nameEn = "Important, Not Urgent",
            colorHex = "#FFC828",
            priority = 2
        ),
        QuadrantConfig(
            id = "III",
            nameKo = "중요하지 않지만 급한 일",
            nameEn = "Urgent, Not Important",
            colorHex = "#3DA5F5",
            priority = 1
        ),
        QuadrantConfig(
            id = "IV",
            nameKo = "Not Urgent & Unimportant",
            nameEn = "Not Urgent & Unimportant",
            colorHex = "#4CD267",
            priority = 0
        )
    )
)

fun QuadrantConfig.priorityLabel(): String = when (priority) {
    3 -> "높은 우선도"
    2 -> "중간 우선도"
    1 -> "낮은 우선도"
    0 -> "우선도 없음"
    else -> "전체"
}
