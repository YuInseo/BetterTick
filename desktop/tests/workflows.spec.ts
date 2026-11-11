import { test, expect, type Page } from "@playwright/test";
const nav = async (page: Page, name: string) =>
  page.locator(".sidebar .nav-item").filter({ hasText: name }).first().click();
const add = async (page: Page, title: string) => {
  await page.locator("#quick-add").fill(title);
  await page.locator(".add-task button").click();
};
const closeDetail = async (page: Page) => {
  if (await page.locator(".detail").count())
    await page.locator(".detail-top button").click();
};
test.beforeEach(async ({ page }) => {
  await page.goto("/");
  await nav(page, "전체");
});
test("local tasks, notes, subtasks, lists and tags survive reload", async ({
  page,
}) => {
  await add(page, "Persistent Task #work !3");
  await page.locator(".notes").fill("Saved memo");
  await page.locator(".detail-footer .primary").click();
  await page.reload();
  await nav(page, "전체");
  await page.locator(".task-body").click();
  await expect(page.locator(".detail-title")).toHaveValue("Persistent Task");
  await expect(page.locator(".notes")).toHaveValue("Saved memo");
  await expect(page.locator(".task-tags").first()).toContainText("work");
  await expect(
    page
      .locator(".detail label")
      .filter({ hasText: "우선순위" })
      .locator("select"),
  ).toHaveValue("3");
});
test("trash restore and permanent delete are distinct operations", async ({
  page,
}) => {
  await add(page, "Recover me");
  page.once("dialog", (d) => d.accept());
  await page.locator(".detail-footer .danger").click();
  await nav(page, "휴지통");
  await expect(page.locator(".task-body")).toContainText("Recover me");
  await page.getByRole("button", { name: "복원", exact: true }).click();
  await expect(page.locator(".tasks .task")).toHaveCount(0);
  await nav(page, "전체");
  await expect(page.locator(".task-body")).toContainText("Recover me");
  await page.locator(".task-body").click();
  page.once("dialog", (d) => d.accept());
  await page.locator(".detail-footer .danger").click();
  await nav(page, "휴지통");
  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: "영구 삭제", exact: true }).click();
  await page.reload();
  await nav(page, "휴지통");
  await expect(page.locator(".tasks .task")).toHaveCount(0);
});
test("quick add tomorrow, priority and tag filter", async ({ page }) => {
  await add(page, "Report 내일 #업무 !3");
  await expect(page.locator(".detail-title")).toHaveValue("Report");
  await closeDetail(page);
  await nav(page, "예정");
  await expect(page.locator(".task-body")).toContainText("Report");
  await nav(page, "태그");
  await page.locator(".tag-name").click();
  await expect(page.locator(".task-body")).toContainText("Report");
  await expect(page.getByLabel("태그 필터")).not.toHaveValue("");
});
test("tags can be created, renamed, recolored and deleted without Firebase", async ({
  page,
}) => {
  await nav(page, "태그");
  page.once("dialog", (d) => d.accept("Personal"));
  await page.getByRole("button", { name: "+ 태그 추가", exact: true }).click();
  await expect(page.locator(".tag-row")).toContainText("Personal");
  page.once("dialog", (d) => d.accept("Home"));
  await page.getByRole("button", { name: "이름 변경", exact: true }).click();
  await page.getByLabel("Home 색상").fill("#ff0000");
  await page.reload();
  await nav(page, "태그");
  await expect(page.getByLabel("Home 색상")).toHaveValue("#ff0000");
  page.once("dialog", (d) => d.accept());
  await page.locator(".tag-row .danger").click();
  await expect(page.locator(".tag-row")).toHaveCount(0);
});
test("quantified habits log progress, persist and archive", async ({
  page,
}) => {
  await nav(page, "습관");
  await page.getByRole("button", { name: "+ 습관 추가", exact: true }).click();
  await page.getByLabel("습관 이름", { exact: true }).fill("Drink Water");
  await page.getByLabel("습관 목표").fill("2");
  await page.getByRole("button", { name: "습관 저장" }).click();
  const cell = page.locator(".habit-row .habit-check").last();
  await cell.click();
  await expect(cell).toHaveText("1/2");
  await cell.click();
  await expect(cell).toHaveText("✓");
  await page.reload();
  await nav(page, "습관");
  await expect(page.locator(".habit-row .habit-check").last()).toHaveText("✓");
  await page.getByRole("button", { name: "보관", exact: true }).click();
  await expect(page.locator(".habit-row")).toHaveCount(0);
  await page.getByRole("checkbox", { name: "보관한 습관" }).check();
  await page.getByRole("button", { name: "복원", exact: true }).click();
  await page.getByRole("checkbox", { name: "보관한 습관" }).uncheck();
  await expect(page.locator(".habit-row")).toContainText("Drink Water");
});
test("recurring completion produces one successor and resets checklist", async ({
  page,
}) => {
  await add(page, "Recurring");
  await page.locator(".detail input[type=date]").first().fill("2026-01-31");
  await page
    .locator(".detail label")
    .filter({ hasText: "반복" })
    .locator("select")
    .selectOption("MONTHLY");
  await page.getByLabel("하위 할 일 입력").fill("Child");
  await page.locator(".subtask-add button").click();
  await page.getByRole("checkbox", { name: "완료 Child", exact: true }).check();
  await closeDetail(page);
  await page.getByTitle("완료 전환").click();
  await expect(page.locator(".tasks .task")).toHaveCount(1);
  await page.locator(".task-body").click();
  await expect(page.locator(".detail input[type=date]").first()).toHaveValue(
    "2026-02-28",
  );
  await expect(
    page.getByRole("checkbox", { name: "완료 Child", exact: true }),
  ).not.toBeChecked();
  await closeDetail(page);
  await nav(page, "완료");
  await page.getByTitle("완료 전환").click();
  await nav(page, "전체");
  await page
    .locator(".task")
    .filter({ hasText: "2026-01-31" })
    .getByTitle("완료 전환")
    .click();
  await expect(page.locator(".tasks .task")).toHaveCount(1);
});
test("list removal moves tasks into inbox", async ({ page }) => {
  page.once("dialog", (d) => d.accept("Disposable List"));
  await page.getByTitle("목록 추가").click();
  await add(page, "Listed Task");
  await closeDetail(page);
  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: "목록 삭제", exact: true }).click();
  await expect(page.locator(".task-body")).toContainText("Listed Task");
  await expect(page.locator(".sidebar")).not.toContainText("Disposable List");
  await page.reload();
  await nav(page, "받은 편지함");
  await expect(page.locator(".task-body")).toContainText("Listed Task");
});
test("batch selection completes multiple tasks", async ({ page }) => {
  await add(page, "Batch A");
  await closeDetail(page);
  await add(page, "Batch B");
  await closeDetail(page);
  await page.getByLabel("선택 Batch A", { exact: true }).check();
  await page.getByLabel("선택 Batch B", { exact: true }).check();
  await page.getByRole("button", { name: "선택 완료 전환" }).click();
  await expect(page.locator(".tasks .task")).toHaveCount(0);
  await nav(page, "완료");
  await expect(page.locator(".tasks .task")).toHaveCount(2);
});
test("calendar drag reschedules and week start preference applies", async ({
  page,
}) => {
  await add(page, "Drag Task");
  await closeDetail(page);
  await nav(page, "캘린더");
  const target = page.locator(".day:not(.muted)").nth(10);
  const key = await target.getAttribute("data-date");
  await page.locator(".unscheduled .calendar-task").dragTo(target);
  await target.locator(".calendar-task").click();
  await expect(page.locator(".detail input[type=date]").first()).toHaveValue(
    key!,
  );
  await closeDetail(page);
  await page.locator(".profile .icon-button").click();
  await page.locator(".settings-options select").nth(1).selectOption("1");
  await nav(page, "캘린더");
  await expect(page.locator(".weekday").first()).toHaveText("월");
});
test("week calendar drag sets task time", async ({ page }) => {
  await add(page, "Time Block");
  await closeDetail(page);
  await nav(page, "캘린더");
  await page.getByRole("button", { name: "주", exact: true }).click();
  const slot = page.locator("[data-slot]").nth(7);
  const key = await slot.getAttribute("data-slot");
  await page.locator(".unscheduled .calendar-task").dragTo(slot);
  await slot.locator(".calendar-task").click();
  await expect(page.getByLabel("마감 시간")).toHaveValue(key!.slice(11));
});
test("kanban and matrix drags change priority and urgency", async ({
  page,
}) => {
  await add(page, "Move Card");
  await closeDetail(page);
  await nav(page, "칸반 보드");
  await page
    .locator(".board-card")
    .dragTo(page.locator(".board-column").nth(3));
  await expect(page.locator(".board-column").nth(3)).toContainText("Move Card");
  await nav(page, "아이젠하워");
  await page
    .locator(".matrix-cell button")
    .dragTo(page.locator(".matrix-cell").first());
  await page
    .locator(".matrix-cell")
    .first()
    .getByRole("button", { name: "Move Card" })
    .click();
  await expect(
    page
      .locator(".detail label")
      .filter({ hasText: "우선순위" })
      .locator("select"),
  ).toHaveValue("3");
});
test("export and import include tags habits sessions and preferences", async ({
  page,
}) => {
  await add(page, "Backup Task #keep");
  await closeDetail(page);
  const downloadPromise = page.waitForEvent("download");
  await page.getByTitle("백업 내보내기").click();
  const download = await downloadPromise;
  const path = await download.path();
  expect(path).toBeTruthy();
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  page.once("dialog", (d) => d.accept());
  await page.locator(".backup-import input").setInputFiles(path!);
  await expect(page.locator(".toast")).toContainText("백업을 가져왔습니다");
  await nav(page, "전체");
  await expect(page.locator(".task-body")).toContainText("Backup Task");
  await nav(page, "태그");
  await expect(page.locator(".tag-row")).toContainText("keep");
});
test("malformed backup leaves existing tasks untouched", async ({ page }) => {
  await add(page, "Keep me");
  await closeDetail(page);
  await page.locator(".backup-import input").setInputFiles({
    name: "bad.json",
    mimeType: "application/json",
    buffer: Buffer.from(
      JSON.stringify({
        format: "bettertick-backup",
        version: 1,
        tasks: [{ id: "bad", title: "bad" }],
        lists: [],
      }),
    ),
  });
  await expect(page.locator(".toast")).toContainText("가져오기 실패");
  await expect(page.locator(".task-body")).toContainText("Keep me");
});
test("focus countdown survives reload and records exactly once while another view is open", async ({
  page,
}) => {
  await page.clock.install();
  await nav(page, "집중");
  await page.getByRole("button", { name: "시작", exact: true }).click();
  await page.clock.fastForward(5000);
  await page.reload();
  await nav(page, "집중");
  await expect(page.locator(".timer")).not.toHaveText("25:00");
  await nav(page, "전체");
  await page.clock.fastForward(25 * 60000);
  await nav(page, "집중");
  await expect(page.locator(".session-list>div")).toHaveCount(1);
  await page.reload();
  await nav(page, "집중");
  await expect(page.locator(".session-list>div")).toHaveCount(1);
});
test("stopwatch pause resume and recorded session", async ({ page }) => {
  await page.clock.install();
  await nav(page, "집중");
  await page.getByRole("button", { name: "스톱워치", exact: true }).click();
  await page.getByRole("button", { name: "시작", exact: true }).click();
  await page.clock.fastForward(10000);
  await page.getByRole("button", { name: "일시정지", exact: true }).click();
  await expect(page.locator(".timer")).toHaveText("00:10");
  await page.clock.fastForward(5000);
  await expect(page.locator(".timer")).toHaveText("00:10");
  await page.getByRole("button", { name: "시작", exact: true }).click();
  await page.clock.fastForward(10000);
  await page.getByRole("button", { name: "기록하고 종료" }).click();
  await expect(page.locator(".session-list>div")).toHaveCount(1);
  const sessions = await page.evaluate(() =>
    JSON.parse(
      localStorage.getItem("bettertick.desktop.data.v1.local.sessions") || "[]",
    ),
  );
  expect(sessions[0].seconds).toBe(20);
});
test("smart filter persists priority constraint", async ({ page }) => {
  await add(page, "High !3");
  await closeDetail(page);
  await add(page, "Low !1");
  await closeDetail(page);
  page.once("dialog", (d) => d.accept("Important"));
  await page.getByTitle("필터 추가").click();
  await nav(page, "Important");
  await page.getByLabel("필터 우선순위").selectOption("3");
  await expect(page.locator(".task-body")).toHaveCount(1);
  await expect(page.locator(".task-body")).toContainText("High");
  await page.reload();
  await nav(page, "Important");
  await expect(page.locator(".task-body")).toHaveCount(1);
});
test("every main navigation renders without runtime errors", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  for (const name of [
    "오늘",
    "예정",
    "받은 편지함",
    "전체",
    "완료",
    "휴지통",
    "캘린더",
    "태그",
    "습관",
    "아이젠하워",
    "칸반 보드",
    "집중",
    "통계",
  ]) {
    await nav(page, name);
    await expect(page.locator(".main")).toBeVisible();
  }
  expect(errors).toEqual([]);
  await page.setViewportSize({ width: 900, height: 700 });
  await nav(page, "전체");
  await add(page, "Visual QA");
  await page.screenshot({
    path: "test-results/desktop-task-detail.png",
    fullPage: true,
  });
  await closeDetail(page);
  await nav(page, "습관");
  await page.screenshot({
    path: "test-results/desktop-habits.png",
    fullPage: true,
  });
});

test("notes render safe Markdown and attachment downloads persist", async ({
  page,
}) => {
  await nav(page, "노트");
  await add(page, "Meeting Notes");
  await page
    .locator(".notes")
    .fill("## Agenda\n\n- [x] Review\n\n<script>alert(1)</script>");
  await page.locator(".detail-footer .primary").click();
  await page.getByRole("button", { name: "미리보기", exact: true }).click();
  await expect(page.locator(".markdown h2")).toHaveText("Agenda");
  await expect(page.locator(".markdown script")).toHaveCount(0);
  await page.getByLabel("첨부 파일 추가", { exact: true }).setInputFiles({
    name: "memo.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("Memo"),
  });
  await expect(page.locator(".attachment a")).toHaveText("memo.txt");
  await page.reload();
  await nav(page, "노트");
  await page.locator(".task-body").click();
  await expect(page.locator(".attachment a")).toHaveAttribute(
    "download",
    "memo.txt",
  );
});
test("countdowns create edit and persist", async ({ page }) => {
  await nav(page, "디데이");
  await page.getByRole("button", { name: "디데이 추가" }).click();
  await page.getByLabel("디데이 이름").fill("Launch");
  await page.getByLabel("디데이 날짜").fill("2030-01-01");
  await page.getByRole("button", { name: "디데이 저장" }).click();
  await expect(page.locator(".countdown-grid")).toContainText("Launch");
  await page.reload();
  await nav(page, "디데이");
  await expect(page.locator(".countdown-grid")).toContainText("Launch");
  await page.locator(".countdown-edit").click();
  await page.getByLabel("디데이 이름").fill("Release");
  await page.getByRole("button", { name: "디데이 저장" }).click();
  await expect(page.locator(".countdown-grid")).toContainText("Release");
});
test("English setting updates navigation and survives reload", async ({
  page,
}) => {
  await page.locator(".profile .icon-button").click();
  await page.locator(".settings-options select").first().selectOption("en");
  await expect(page.locator(".sidebar")).toContainText("Today");
  await expect(
    page.getByRole("heading", { name: "Settings", exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(page.locator(".sidebar")).toContainText("Today");
  await page
    .locator(".sidebar .nav-item")
    .filter({ hasText: "Calendar" })
    .click();
  await page.getByRole("button", { name: "Year", exact: true }).click();
  await expect(page.locator(".year-calendar article")).toHaveCount(12);
});
