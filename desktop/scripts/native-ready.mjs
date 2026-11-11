import { chromium, expect } from "@playwright/test";
import { setTimeout as delay } from "node:timers/promises";
let browser;
try {
  const deadline = Date.now() + 45000;
  while (!browser && Date.now() < deadline) {
    try {
      browser = await chromium.connectOverCDP("http://127.0.0.1:9333", {
        timeout: 2000,
      });
    } catch {
      await delay(250);
    }
  }
  if (!browser) throw Error("WebView2 debugging endpoint did not become ready");
  let page;
  while (!page && Date.now() < deadline) {
    page = browser
      .contexts()
      .flatMap((c) => c.pages())
      .find((p) => !p.url().startsWith("devtools:"));
    if (!page) await delay(100);
  }
  if (!page) throw Error("Native WebView page did not become ready");
  await page
    .locator("#quick-add")
    .waitFor({ state: "visible", timeout: 30000 });
  await page.locator("#quick-add").fill("Native UI QA");
  await page.locator(".add-task button").click();
  await expect(page.locator(".detail-title")).toHaveValue("Native UI QA");
  await page.screenshot({
    path: "test-results/native-webview.png",
    animations: "disabled",
  });
  await page.locator(".detail-top button").click();
  // The OS process owns this WebView. Do not issue Browser.close through CDP.
  await new Promise((resolve) =>
    process.stdout.write(
      "PASS: Native WebView renders the redesigned app and creates a task.\n",
      resolve,
    ),
  );
  process.exit(0);
} catch (error) {
  console.error(error);
  process.exit(1);
}
