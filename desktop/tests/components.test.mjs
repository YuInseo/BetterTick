import { test, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import { JSDOM } from "jsdom";
import { buildSync } from "esbuild";
import { writeFileSync, unlinkSync } from "node:fs";
const dom = new JSDOM("<!doctype html><html><body></body></html>", {
  url: "http://localhost",
});
for (const key of [
  "window",
  "document",
  "localStorage",
  "HTMLElement",
  "Node",
  "Event",
  "MouseEvent",
  "KeyboardEvent",
  "File",
  "Blob",
  "getComputedStyle",
])
  globalThis[key] =
    key === "getComputedStyle"
      ? dom.window.getComputedStyle.bind(dom.window)
      : dom.window[key];
Object.defineProperty(globalThis, "navigator", {
  value: dom.window.navigator,
  configurable: true,
});
globalThis.matchMedia = () => ({
  matches: false,
  addEventListener() {},
  removeEventListener() {},
});
globalThis.confirm = () => true;
globalThis.prompt = () => null;
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const path = new URL("../.component-test.mjs", import.meta.url);
const result = buildSync({
  entryPoints: [new URL("../src/App.tsx", import.meta.url).pathname],
  bundle: true,
  write: false,
  format: "esm",
  platform: "node",
  packages: "external",
  loader: { ".css": "empty" },
  define: {
    "import.meta.env.VITE_QA_MODE": '"false"',
    "import.meta.env.VITE_FIREBASE_API_KEY": '""',
    "import.meta.env.VITE_FIREBASE_AUTH_DOMAIN": '""',
    "import.meta.env.VITE_FIREBASE_PROJECT_ID": '""',
    "import.meta.env.VITE_FIREBASE_APP_ID": '""',
  },
});
writeFileSync(path, result.outputFiles[0].text);
const { default: App } = await import(path.href);
unlinkSync(path);
const React = await import("react");
const { render, screen, fireEvent, cleanup, within, waitFor } =
  await import("@testing-library/react");
const { default: userEvent } = await import("@testing-library/user-event");
let user;
beforeEach(() => {
  localStorage.clear();
  user = userEvent.setup();
  render(React.createElement(App));
});
afterEach(() => cleanup());
const nav = async (name) => {
  const buttons = document.querySelectorAll(".sidebar .nav-item");
  const button = Array.from(buttons).find((b) => b.textContent.includes(name));
  assert.ok(button);
  await user.click(button);
};
const add = async (title) => {
  await user.type(document.querySelector("#quick-add"), title);
  await user.click(document.querySelector(".add-task button"));
};
test("production mode works without Firebase and persists task edits", async () => {
  await add("Persistent");
  await user.type(document.querySelector(".notes"), "Memo");
  await user.click(document.querySelector(".detail-footer .primary"));
  await waitFor(() =>
    assert.equal(
      JSON.parse(
        localStorage.getItem("bettertick.desktop.data.v1.local.tasks"),
      )[0].notes,
      "Memo",
    ),
  );
  cleanup();
  render(React.createElement(App));
  await nav("전체");
  assert.equal(
    document.querySelector(".task-body span").textContent,
    "Persistent",
  );
});
test("quick add creates a real tag and parses priority", async () => {
  await nav("전체");
  await add("Report 내일 #업무 !3");
  assert.equal(document.querySelector(".detail-title").value, "Report");
  assert.match(document.querySelector(".task-tags").textContent, /업무/);
  assert.equal(document.querySelectorAll(".detail select")[0].value, "3");
  await nav("태그");
  assert.match(document.querySelector(".tag-row").textContent, /업무/);
});
test("trash restore survives remount", async () => {
  await add("Restore");
  await user.click(document.querySelector(".detail-footer .danger"));
  await nav("휴지통");
  await user.click(screen.getByRole("button", { name: "복원", exact: true }));
  await nav("전체");
  assert.match(document.querySelector(".tasks").textContent, /Restore/);
});
test("habit goal progress persists and archives", async () => {
  await nav("습관");
  await user.click(screen.getByRole("button", { name: "+ 습관 추가" }));
  await user.type(screen.getByLabelText("습관 이름"), "Water");
  fireEvent.change(screen.getByLabelText("습관 목표"), {
    target: { value: "2" },
  });
  await user.click(screen.getByRole("button", { name: "습관 저장" }));
  const cell = document.querySelector(".habit-row .habit-check:last-child");
  await user.click(cell);
  assert.equal(cell.textContent, "1/2");
  await user.click(cell);
  assert.equal(cell.textContent, "✓");
  await user.click(screen.getByRole("button", { name: "보관", exact: true }));
  assert.equal(document.querySelectorAll(".habit-row").length, 0);
});
test("list creation and deletion preserve its tasks", async () => {
  globalThis.prompt = () => "Work";
  await user.click(screen.getByTitle("목록 추가"));
  await add("Listed");
  await user.click(document.querySelector(".detail-top button"));
  await user.click(
    screen.getByRole("button", { name: "목록 삭제", exact: true }),
  );
  assert.match(document.querySelector(".tasks").textContent, /Listed/);
  assert.equal(
    JSON.parse(
      localStorage.getItem("bettertick.desktop.data.v1.local.tasks"),
    )[0].listId,
    "",
  );
});
test("every navigation renders a real screen", async () => {
  for (const name of [
    "전체",
    "태그",
    "습관",
    "통계",
    "캘린더",
    "아이젠하워",
    "칸반 보드",
    "집중",
  ]) {
    await nav(name);
    assert.ok(document.querySelector(".main"));
  }
  assert.match(document.querySelector(".focus").textContent, /집중 타이머/);
});
