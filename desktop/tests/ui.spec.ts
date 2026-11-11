import { test, expect } from "@playwright/test";
test.beforeEach(async ({ page }) => {
  await page.goto("/");
  await expect(
    page.getByText("BetterTick", { exact: true }).first(),
  ).toBeVisible();
});
test("add, complete, uncomplete and delete a task", async ({ page }) => {
  await page.locator("#quick-add").fill("QA sample task");
  await page.getByRole("button", { name: "추가", exact: true }).click();
  await expect(
    page.locator(".task-body").filter({ hasText: "QA sample task" }).first(),
  ).toBeVisible();
  await page.getByTitle("완료 전환").click();
  await page.locator(".nav-item").filter({ hasText: "완료" }).first().click();
  await expect(
    page.locator(".task-body").filter({ hasText: "QA sample task" }).first(),
  ).toBeVisible();
  await page.getByTitle("완료 전환").click();
  await page.locator(".nav-item").filter({ hasText: "전체" }).first().click();
  await page
    .locator(".task-body")
    .filter({ hasText: "QA sample task" })
    .first()
    .click();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "삭제", exact: true }).click();
  await expect(page.getByText("QA sample task")).toHaveCount(0);
});
test("calendar next and previous buttons navigate correctly", async ({
  page,
}) => {
  await page
    .locator(".sidebar-bottom .nav-item")
    .filter({ hasText: "캘린더" })
    .click();
  const now = new Date();
  const next = new Date(now.getFullYear(), now.getMonth() + 1, 1);
  await page.locator(".heading .actions button").last().click();
  await expect(
    page.getByText(next.getFullYear() + "년 " + (next.getMonth() + 1) + "월"),
  ).toBeVisible();
  await page.locator(".heading .actions button").first().click();
  await expect(
    page.getByText(now.getFullYear() + "년 " + (now.getMonth() + 1) + "월"),
  ).toBeVisible();
});
test("focus timer starts, pauses and resets", async ({ page }) => {
  await page
    .locator(".sidebar-bottom .nav-item")
    .filter({ hasText: "집중" })
    .click();
  await expect(page.locator(".timer")).toHaveText("25:00");
  await page.getByRole("button", { name: "시작", exact: true }).click();
  await expect(page.getByRole("button", { name: "일시정지" })).toBeVisible();
  await page.getByRole("button", { name: "초기화" }).click();
  await expect(page.locator(".timer")).toHaveText("25:00");
});
test("sidebar can close and reopen", async ({ page }) => {
  await page.getByTitle("사이드바 닫기").click();
  await expect(page.locator(".sidebar")).toHaveCount(0);
  await page.locator(".toolbar .icon-button").first().click();
  await expect(page.locator(".sidebar")).toBeVisible();
});
test("new list creation and navigation", async ({ page }) => {
  page.on("dialog", (dialog) => dialog.accept("QA List"));
  await page.getByTitle("목록 추가").click();
  await expect(
    page.locator(".nav-item").filter({ hasText: "QA List" }),
  ).toBeVisible();
});

test("settings persist theme and default priority across reload", async ({
  page,
}) => {
  await page.locator(".profile .icon-button").click();
  await expect(page.getByRole("heading", { name: "설정" })).toBeVisible();
  await page.getByRole("button", { name: "화면", exact: true }).click();
  await page.locator(".settings-options select").first().selectOption("dark");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.getByRole("button", { name: "할 일", exact: true }).click();
  await page.locator(".settings-options select").first().selectOption("3");
  await page.reload();
  await page.locator(".profile .icon-button").click();
  await page.getByRole("button", { name: "할 일", exact: true }).click();
  await expect(page.locator(".settings-options select").first()).toHaveValue(
    "3",
  );
});
test("settings reset returns default values", async ({ page }) => {
  await page.locator(".profile .icon-button").click();
  await page
    .locator(".settings-layout nav")
    .getByRole("button", { name: "집중", exact: true })
    .click();
  await page.locator(".settings-options input[type=number]").first().fill("35");
  await page.getByRole("button", { name: "기본값 복원" }).click();
  await expect(
    page.locator(".settings-options input[type=number]").first(),
  ).toHaveValue("25");
});

test("keyboard shortcuts focus search and new-task input", async ({ page }) => {
  await page.keyboard.press("Control+k");
  await expect(page.locator("#search")).toBeFocused();
  await page.keyboard.press("Control+n");
  await expect(page.locator("#quick-add")).toBeFocused();
});
test("task details priority and repeat settings can be changed", async ({
  page,
}) => {
  await page.locator("#quick-add").fill("Detail QA");
  await page.getByRole("button", { name: "추가", exact: true }).click();
  await expect(page.locator(".detail")).toBeVisible();
  await page
    .locator(".detail label")
    .filter({ hasText: "우선순위" })
    .locator("select")
    .selectOption("3");
  await expect(
    page
      .locator(".detail label")
      .filter({ hasText: "우선순위" })
      .locator("select"),
  ).toHaveValue("3");
  await page
    .locator(".detail label")
    .filter({ hasText: "반복" })
    .locator("select")
    .selectOption("DAILY");
  await expect(
    page.locator(".detail label").filter({ hasText: "반복" }).locator("select"),
  ).toHaveValue("DAILY");
});

test("subtask checklist can be added, checked and removed", async ({
  page,
}) => {
  await page.locator("#quick-add").fill("Parent QA");
  await page.getByRole("button", { name: "추가", exact: true }).click();
  await page.getByRole("textbox", { name: "하위 할 일 입력" }).fill("Child QA");
  await page.locator(".subtask-add button").click();
  await expect(
    page.locator(".subtask").filter({ hasText: "Child QA" }),
  ).toBeVisible();
  await page.getByRole("checkbox", { name: "완료 Child QA" }).check();
  await expect(
    page.locator(".subtask").filter({ hasText: "Child QA" }).locator("span"),
  ).toHaveCSS("text-decoration-line", "line-through");
  await page.getByRole("button", { name: "삭제 Child QA" }).click();
  await expect(page.locator(".subtask")).toHaveCount(0);
});
