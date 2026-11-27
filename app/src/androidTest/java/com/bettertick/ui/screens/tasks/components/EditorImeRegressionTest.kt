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
                            TaskMemo(id = "second", kind = com.bettertick.data.model.NoteBlockKind.TODO, text = "second\n"),
                            TaskMemo(id = "third", kind = "text", text = "third"))),
                    listName = "기본함", parentTitle = "야간",
                    occurrenceDate = LocalDate.of(2026, 11, 15),
                    onDismiss = { open.value = false }, onUpdateTask = { saved = it }, onToggleComplete = {})
            }
        }
        // Full page: drag the handle up (there is no expand button any more).
        if (page) {
            fun surfaceHeight() = compose.onNodeWithTag("task-detail-surface").fetchSemanticsNode().boundsInRoot.height
            val drawerHeight = surfaceHeight()
            compose.onNodeWithTag("task-detail-header").performTouchInput {
                swipe(center, androidx.compose.ui.geometry.Offset(center.x, center.y - 3000f), durationMillis = 250L)
            }
            // The drawer is 78 % of the page height; wait for the full page.
            compose.waitUntil(5_000) { surfaceHeight() >= drawerHeight / 0.78f - 4f }
        }
        compose.waitForIdle()
    }

    @Test fun drawerKeepsItsHeaderAndFooterWhenKeyboardOpens() = checkKeyboard(false)
    @Test fun pageKeepsItsHeaderAndFooterWhenKeyboardOpens() = checkKeyboard(true)

    @Test fun checklistActionsAreHiddenUntilBlockManagementOpens() {
        showEditor(false)
        val actions = compose.onAllNodesWithContentDescription("블록 메뉴")
        actions.assertCountEquals(0)
        val checkbox = compose.onNode(isToggleable() and hasContentDescription("second", substring = true))
        checkbox.assertIsOff().performClick()
        checkbox.assertIsOn()
        val paragraph = compose.onNodeWithTag("note-block-first").fetchSemanticsNode().boundsInRoot
        val marker = checkbox.fetchSemanticsNode().boundsInRoot
        assertEquals("checkbox aligns with paragraph", paragraph.left, marker.left, 1f)

        compose.onNodeWithContentDescription("더 보기").performClick()
        compose.onNodeWithText("블록 관리").performClick()
        actions.assertCountEquals(3)
        actions[1].performClick()
        compose.onNodeWithText("복제").performClick()
        actions.assertCountEquals(4)
        compose.onNodeWithContentDescription("더 보기").performClick()
        compose.onNodeWithText("블록 관리 종료").performClick()
        actions.assertCountEquals(0)
    }

    private fun checkKeyboard(page: Boolean) {
        showEditor(page)
        val density = compose.activity.resources.displayMetrics.density
        val first = compose.onNodeWithTag("note-block-first").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("note-block-second").fetchSemanticsNode().boundsInRoot
        assertTrue("single-line block gap must stay compact: ${second.top - first.top}", second.top - first.top <= 32f * density)
        val beforeTop = compose.onNodeWithTag("task-detail-header").fetchSemanticsNode().boundsInRoot.top + appBounds().top
        // A tap that lands before the editor dialog's window has input focus can
        // focus the field without the keyboard ever being shown (seen as flaky
        // timeouts here). Tap again until the keyboard is up, within 20 s.
        val keyboardDeadline = System.currentTimeMillis() + 20_000
        while (imeBounds() == null && System.currentTimeMillis() < keyboardDeadline) {
            compose.onNodeWithTag("note-block-third").performClick()
            runCatching { compose.waitUntil(4_000) { imeBounds() != null } }
        }
        if (imeBounds() == null) fail("keyboard never opened after tapping the block; " + imeDiagnostics())
        compose.waitForIdle()
        // The bar right above the keyboard is the Notion-style block bar now
        // (the old bottom toolbar is gone).
        fun bar() = runCatching { compose.onNodeWithTag("block-keyboard-bar").fetchSemanticsNode().boundsInRoot }.getOrNull()
        val settled = runCatching {
            compose.waitUntil(15_000) {
                val keyboard = imeBounds()
                val footer = bar()
                keyboard != null && footer != null && keyboard.top - (footer.bottom + appBounds().top) in (-2f * density)..(24f * density)
            }
        }.isSuccess
        val geometry = "mode=$page ime=${imeBounds()} window=${appBounds()} bar=${bar()} " +
            "actions=${runCatching { compose.onNodeWithTag("task-detail-actions").fetchSemanticsNode().boundsInRoot }.getOrNull()} density=$density"
        println("UI_GEOMETRY $geometry")
        capture("${if (page) "page" else "drawer"}_keyboard")
        assertTrue("block bar must sit right above the keyboard: $geometry", settled)
        val ime = requireNotNull(imeBounds())
        val appWindow = instrumentation.uiAutomation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val windowBounds = Rect().also { appWindow?.getBoundsInScreen(it) }
        assertTrue("dialog must not pan above the display: $windowBounds", windowBounds.top >= 0)
        val header = compose.onNodeWithTag("task-detail-header").fetchSemanticsNode().boundsInRoot
        val surface = runCatching { compose.onNodeWithTag("task-detail-surface").fetchSemanticsNode().boundsInRoot }.getOrNull()
        assertTrue("keyboard must not move the header: before=$beforeTop now=${header.top + windowBounds.top} " +
            "header=$header window=$windowBounds surface=$surface ime=$ime density=$density",
            abs(header.top + windowBounds.top - beforeTop) <= 2f * density)
        val footer = compose.onNodeWithTag("block-keyboard-bar").fetchSemanticsNode().boundsInRoot
        val footerBottom = footer.bottom + windowBounds.top
        assertTrue("toolbar must stay above keyboard: $footerBottom vs ${ime.top}", footerBottom <= ime.top + 2f * density)
        assertTrue("no calendar-sized gap above keyboard: ${ime.top - footerBottom}", ime.top - footerBottom <= 24f * density)
        val flag = compose.onNodeWithContentDescription("우선순위").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithContentDescription("더 보기").fetchSemanticsNode().boundsInRoot
        if (page) {
            // 전체 페이지: 날짜 줄이 뒤로가기 옆 고정 바로 올라간다.
            assertTrue("flag sits in the pinned bar", flag.top >= header.top - 2 && flag.bottom <= header.bottom + 2)
            assertTrue("more sits in the pinned bar", more.top >= header.top - 2 && more.bottom <= header.bottom + 2)
        } else {
            // 우선순위 · 더보기는 헤더가 아니라 그 아래 날짜 줄에 나란히 있다.
            assertTrue("flag sits below the header", flag.top >= header.bottom - 2)
            assertTrue("more sits below the header", more.top >= header.bottom - 2)
        }
        assertTrue("flag and more share the date row",
            abs((flag.top + flag.bottom) / 2 - (more.top + more.bottom) / 2) <= 2f * density)
        compose.onNodeWithTag("note-block-third").performTextInput(" edited")
        // No 저장 button any more: closing the sheet saves. Back first hides the
        // keyboard, the next one dismisses the editor.
        repeat(3) {
            if (saved == null) {
                instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                compose.waitForIdle()
            }
        }
        compose.runOnIdle { assertTrue(saved?.memos?.any { it.id == "third" && it.text.contains("edited") } == true) }
    }

    /** Default IME and input-method service state, for a failure message. */
    private fun imeDiagnostics(): String {
        fun shell(cmd: String): String = runCatching {
            val pfd = instrumentation.uiAutomation.executeShellCommand(cmd)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val default = shell("settings get secure default_input_method").trim()
        val state = shell("dumpsys input_method").lines()
            .filter { line -> listOf("mCurMethodId", "mInputShown", "mServedView", "mCurFocusedWindow", "mIsInputViewShown").any { it in line } }
            .take(8).joinToString(" | ") { it.trim() }
        return "default=$default; $state"
    }

    private fun imeBounds(): Rect? = instrumentation.uiAutomation.windows.firstOrNull {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }?.let { window -> Rect().also { window.getBoundsInScreen(it) }.takeIf { it.height() > 0 } }

    @After fun screenshotOnExit() { capture("${testName.methodName}_end") }

    private fun appBounds(): Rect = Rect().also { bounds ->
        instrumentation.uiAutomation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }?.getBoundsInScreen(bounds)
    }

    private fun capture(name: String) {
        runCatching {
            // Use a shell screenshot so failures are retained even if bitmap capture is unavailable.
            val command = "mkdir -p /data/local/tmp/editor-ui && screencap -p /data/local/tmp/editor-ui/$name.png"
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        }.onFailure { println("UI_CAPTURE_ERROR: $it") }
    }
}
