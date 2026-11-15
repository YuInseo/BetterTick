package com.bettertick.ui.screens.tasks.components

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bettertick.data.model.Task
import com.bettertick.data.model.TaskMemo
import com.bettertick.ui.theme.BetterTickTheme
import com.bettertick.ui.theme.ThemeState
import com.bettertick.ui.theme.TickBlue
import com.google.firebase.Timestamp
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class EditorImeRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val testName = TestName()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var saved: Task? = null

    private fun showEditor(page: Boolean) {
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val open = mutableStateOf(true)
        compose.runOnUiThread { ThemeState.applyTheme("어둠", TickBlue, true) }
        compose.setContent {
            BetterTickTheme {
                if (open.value) TaskDetailSheet(
                    task = Task(id = "fixture", title = "Editor fixture", notes = "first\nsecond\nthird",
                        dueDate = Timestamp(java.util.Date.from(LocalDate.of(2026, 11, 15).atTime(13, 0).atZone(ZoneId.systemDefault()).toInstant())),
                        memos = listOf(TaskMemo(id = "first", kind = "text", text = "first\n\n"),
                            TaskMemo(id = "second", kind = "text", text = "second\n"),
                            TaskMemo(id = "third", kind = "text", text = "third"))),
                    listName = "기본함", parentTitle = "야간",
                    occurrenceDate = LocalDate.of(2026, 11, 15),
                    onDismiss = { open.value = false }, onUpdateTask = { saved = it }, onToggleComplete = {})
            }
        }
        if (page) compose.onNodeWithContentDescription("전체 페이지로 열기").performClick()
        compose.waitForIdle()
    }

    @Test fun drawerKeepsItsHeaderAndFooterWhenKeyboardOpens() = checkKeyboard(false)
    @Test fun pageKeepsItsHeaderAndFooterWhenKeyboardOpens() = checkKeyboard(true)

    private fun checkKeyboard(page: Boolean) {
        showEditor(page)
        val density = compose.activity.resources.displayMetrics.density
        val first = compose.onNodeWithTag("note-block-first").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("note-block-second").fetchSemanticsNode().boundsInRoot
        assertTrue("single-line block gap must stay compact: ${second.top - first.top}", second.top - first.top <= 32f * density)
        val beforeTop = compose.onNodeWithTag("task-detail-header").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("note-block-third").performClick()
        compose.waitUntil(15_000) { imeBounds() != null }
        compose.waitForIdle()
        capture("${if (page) "page" else "drawer"}_keyboard")
        val ime = requireNotNull(imeBounds())
        val appWindow = instrumentation.uiAutomation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val windowBounds = Rect().also { appWindow?.getBoundsInScreen(it) }
        assertTrue("dialog must not pan above the display: $windowBounds", windowBounds.top >= 0)
        val header = compose.onNodeWithTag("task-detail-header").fetchSemanticsNode().boundsInRoot
        assertTrue("keyboard must not move the header", abs(header.top - beforeTop) <= 2f * density)
        val footer = compose.onNodeWithTag("task-detail-actions").fetchSemanticsNode().boundsInRoot
        val footerBottom = footer.bottom + windowBounds.top
        assertTrue("toolbar must stay above keyboard: $footerBottom vs ${ime.top}", footerBottom <= ime.top + 2f * density)
        assertTrue("no calendar-sized gap above keyboard: ${ime.top - footerBottom}", ime.top - footerBottom <= 24f * density)
        val flag = compose.onNodeWithContentDescription("우선순위").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithContentDescription("더 보기").fetchSemanticsNode().boundsInRoot
        assertTrue(flag.top >= header.top && flag.bottom <= header.bottom + 2)
        assertTrue(more.top >= header.top && more.bottom <= header.bottom + 2)
        compose.onNodeWithTag("note-block-third").performTextInput(" edited")
        compose.onNodeWithText("저장", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertTrue(saved?.memos?.any { it.id == "third" && it.text.contains("edited") } == true) }
    }

    private fun imeBounds(): Rect? = instrumentation.uiAutomation.windows.firstOrNull {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }?.let { window -> Rect().also { window.getBoundsInScreen(it) }.takeIf { it.height() > 0 } }

    @After fun screenshotOnExit() { capture("${testName.methodName}_end") }

    private fun capture(name: String) {
        runCatching {
            val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
            val scaled = Bitmap.createScaledBitmap(bitmap, 540, (bitmap.height * 540f / bitmap.width).toInt(), true)
            val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-regression").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            scaled.recycle(); bitmap.recycle()
        }
    }
}
