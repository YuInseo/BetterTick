import { test, expect } from "@playwright/test";
test("redesigned workspace remains usable across desktop and phone widths", async ({
  page,
}) => {
  await page.addInitScript(() => {
    const now = new Date();
    now.setHours(12, 0, 0, 0);
    localStorage.setItem(
      "bettertick.desktop.preferences.v1",
      JSON.stringify({ theme: "dark", accent: "#c8f58b" }),
    );
    localStorage.setItem(
      "bettertick.desktop.data.v1.local.lists",
      JSON.stringify([
        { id: "clover", name: "Clover", color: "#92b4ce", sortOrder: 0 },
        { id: "bearwalk", name: "BearWalk", color: "#c5ab7b", sortOrder: 1 },
      ]),
    );
    localStorage.setItem(
      "bettertick.desktop.data.v1.local.tags",
      JSON.stringify([{ id: "dev", name: "개발", color: "#92b4ce" }]),
    );
    localStorage.setItem(
      "bettertick.desktop.data.v1.local.tasks",
      JSON.stringify([
        {
          id: "first",
          title: "Clover 채팅 오류 확인",
          listId: "clover",
          priority: 3,
          isCompleted: false,
          dueDate: now.toISOString(),
          dueTime: "10:00",
          tagIds: ["dev"],
          notes: "메시지 전송과 재접속을 확인해요.",
          subtasks: [{ id: "child", title: "연결 확인", done: false }],
          sortOrder: 1,
        },
        {
          id: "second",
          title: "BearWalk 홈 화면 정리",
          listId: "bearwalk",
          priority: 2,
          isCompleted: false,
          dueDate: now.toISOString(),
          dueTime: "14:00",
          sortOrder: 2,
        },
        {
          id: "third",
          title: "오늘의 목표 정리",
          listId: "",
          priority: 0,
          isCompleted: true,
          dueDate: now.toISOString(),
          sortOrder: 3,
        },
      ]),
    );
  });
  await page.goto("/");
  for (const [width, height] of [
    [1440, 900],
    [900, 760],
    [390, 844],
  ]) {
    await page.setViewportSize({ width, height });
    await expect(
      page.getByRole("heading", { name: "오늘", exact: true }),
    ).toBeInViewport();
    await expect(
      page.getByRole("button", { name: "새 할 일", exact: true }),
    ).toBeInViewport();
    await page.getByRole("button", { name: "새 할 일", exact: true }).click();
    await expect(page.locator("#quick-add")).toBeFocused();
    await page.screenshot({
      path: `test-results/modern-workspace-${width}.png`,
      fullPage: true,
      animations: "disabled",
    });
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth,
    );
    expect(overflow).toBe(false);
  }
  await page.setViewportSize({ width: 1440, height: 900 });
  await page
    .locator(".task-body")
    .filter({ hasText: "Clover 채팅 오류 확인" })
    .click();
  await expect(page.locator(".detail-title")).toHaveValue(
    "Clover 채팅 오류 확인",
  );
  await page.screenshot({
    path: "test-results/modern-task-detail.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.locator(".detail-top button").click();
  await page.getByRole("button", { name: "테마 전환", exact: true }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await page.screenshot({
    path: "test-results/modern-workspace-light.png",
    fullPage: true,
    animations: "disabled",
  });
  const contrast = await page
    .locator(".sidebar .nav-item.active")
    .evaluate((element) => {
      const style = getComputedStyle(element),
        canvas = document.createElement("canvas");
      canvas.width = 1;
      canvas.height = 1;
      const ctx = canvas.getContext("2d")!;
      const lum = (color: string) => {
        ctx.fillStyle = color;
        ctx.fillRect(0, 0, 1, 1);
        const rgb = Array.from(ctx.getImageData(0, 0, 1, 1).data)
          .slice(0, 3)
          .map((n) => {
            const c = n / 255;
            return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
          });
        return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722;
      };
      const a = lum(style.color),
        b = lum(style.backgroundColor);
      return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    });
  expect(contrast).toBeGreaterThanOrEqual(4.5);
  await page
    .locator(".task")
    .filter({ hasText: "Clover 채팅 오류 확인" })
    .getByTitle("완료 전환")
    .click();
  await expect(
    page.locator(".task-body").filter({ hasText: "Clover 채팅 오류 확인" }),
  ).toHaveCount(0);
  await page
    .locator(".sidebar .nav-item")
    .filter({ hasText: "완료" })
    .first()
    .click();
  await expect(
    page.locator(".task-body").filter({ hasText: "Clover 채팅 오류 확인" }),
  ).toBeVisible();
});
