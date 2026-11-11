import { tr } from "./i18n";
import { useEffect, useState } from "react";
export type Preferences = {
  theme: "system" | "light" | "dark";
  accent: string;
  density: "comfortable" | "compact";
  weekStartsOn: 0 | 1;
  dateFormat: "yyyy-mm-dd" | "mm/dd/yyyy" | "dd/mm/yyyy";
  timeFormat: "12h" | "24h";
  defaultView: "today" | "inbox" | "all";
  defaultPriority: 0 | 1 | 2 | 3;
  defaultDue: "none" | "today" | "tomorrow";
  showCompleted: boolean;
  confirmDelete: boolean;
  focusMinutes: number;
  shortBreakMinutes: number;
  longBreakMinutes: number;
  focusSound: boolean;
  desktopNotifications: boolean;
  startMinimized: boolean;
  autoLaunch: boolean;
  language: "ko" | "en";
};
export const defaults: Preferences = {
  theme: "dark",
  accent: "#c8f58b",
  density: "comfortable",
  weekStartsOn: 0,
  dateFormat: "yyyy-mm-dd",
  timeFormat: "24h",
  defaultView: "today",
  defaultPriority: 0,
  defaultDue: "none",
  showCompleted: false,
  confirmDelete: true,
  focusMinutes: 25,
  shortBreakMinutes: 5,
  longBreakMinutes: 15,
  focusSound: true,
  desktopNotifications: true,
  startMinimized: false,
  autoLaunch: false,
  language: "ko",
};
const storageKey = "bettertick.desktop.preferences.v1";
export function loadPreferences(input?: unknown): Preferences {
  try {
    const raw = input ?? JSON.parse(localStorage.getItem(storageKey) || "{}");
    if (!raw || typeof raw !== "object") return { ...defaults };
    const result = { ...defaults };
    for (const key of Object.keys(defaults) as (keyof Preferences)[]) {
      const value = (raw as Record<string, unknown>)[key];
      if (typeof value === typeof defaults[key])
        (result as unknown as Record<string, unknown>)[key] = value;
    }
    const enums: Partial<Record<keyof Preferences, unknown[]>> = {
      theme: ["system", "light", "dark"],
      density: ["comfortable", "compact"],
      weekStartsOn: [0, 1],
      dateFormat: ["yyyy-mm-dd", "mm/dd/yyyy", "dd/mm/yyyy"],
      timeFormat: ["12h", "24h"],
      defaultView: ["today", "inbox", "all"],
      defaultPriority: [0, 1, 2, 3],
      defaultDue: ["none", "today", "tomorrow"],
      language: ["ko", "en"],
    };
    for (const [key, values] of Object.entries(enums))
      if (!values.includes((result as unknown as Record<string, unknown>)[key]))
        (result as unknown as Record<string, unknown>)[key] = (
          defaults as unknown as Record<string, unknown>
        )[key];
    for (const [key, max] of [
      ["focusMinutes", 180],
      ["shortBreakMinutes", 60],
      ["longBreakMinutes", 120],
    ] as const)
      result[key] = Number.isFinite(result[key])
        ? Math.max(1, Math.min(max, result[key]))
        : defaults[key];
    if (!/^#[0-9a-f]{6}$/i.test(result.accent)) result.accent = defaults.accent;
    return result;
  } catch {
    return { ...defaults };
  }
}
export function savePreferences(p: Preferences) {
  localStorage.setItem(storageKey, JSON.stringify(p));
}
export function applyPreferences(p: Preferences) {
  document.documentElement.lang = p.language;
  const dark =
    p.theme === "dark" ||
    (p.theme === "system" &&
      matchMedia("(prefers-color-scheme: dark)").matches);
  document.documentElement.dataset.theme = dark ? "dark" : "light";
  document.documentElement.dataset.density = p.density;
  document.documentElement.style.setProperty("--accent", p.accent);
  const rgb = p.accent
    .slice(1)
    .match(/.{2}/g)
    ?.map((v) => parseInt(v, 16)) ?? [200, 245, 139];
  const light = rgb[0] * 0.299 + rgb[1] * 0.587 + rgb[2] * 0.114;
  document.documentElement.style.setProperty(
    "--accent-ink",
    light > 160 ? "#17210e" : "#ffffff",
  );
}
export function Settings({
  value,
  onChange,
  onClose,
}: {
  value: Preferences;
  onChange: (p: Preferences) => void;
  onClose: () => void;
}) {
  const [tab, setTab] = useState("general");
  useEffect(() => {
    applyPreferences(value);
    const media = matchMedia("(prefers-color-scheme: dark)");
    const handler = () => applyPreferences(value);
    media.addEventListener("change", handler);
    return () => media.removeEventListener("change", handler);
  }, [value]);
  const [nativeError, setNativeError] = useState("");
  const native = "__TAURI_INTERNALS__" in window;
  const permission = async () => {
    try {
      if (native) {
        const n = await import("@tauri-apps/plugin-notification");
        await n.requestPermission();
      } else if ("Notification" in window)
        await Notification.requestPermission();
    } catch (e) {
      setNativeError(String(e));
    }
  };
  const update = <K extends keyof Preferences>(key: K, next: Preferences[K]) =>
    onChange({ ...value, [key]: next });
  const select = <K extends keyof Preferences>(
    key: K,
    options: [string, Preferences[K]][],
  ) => (
    <select
      value={String(value[key])}
      onChange={(e) =>
        update(key, options.find((o) => String(o[1]) === e.target.value)![1])
      }
    >
      {options.map(([label, v]) => (
        <option key={String(v)} value={String(v)}>
          {label}
        </option>
      ))}
    </select>
  );
  const toggle = (key: keyof Preferences) => (
    <input
      type="checkbox"
      checked={Boolean(value[key])}
      onChange={(e) => onChange({ ...value, [key]: e.target.checked })}
    />
  );
  const number = (key: keyof Preferences, min: number, max: number) => (
    <input
      type="number"
      min={min}
      max={max}
      value={Number(value[key])}
      onChange={(e) =>
        onChange({
          ...value,
          [key]: Math.max(min, Math.min(max, Number(e.target.value) || min)),
        })
      }
    />
  );
  return (
    <section className="settings-page">
      <div className="heading">
        <h1>{tr("설정")}</h1>
        <button onClick={onClose}>{tr("닫기 ×")}</button>
      </div>
      <div className="settings-layout">
        <nav>
          {[
            ["general", tr("일반")],
            ["appearance", tr("화면")],
            ["tasks", tr("할 일")],
            ["focus", tr("집중")],
            ["notifications", tr("알림")],
            ["desktop", tr("데스크톱")],
          ].map(([id, label]) => (
            <button
              key={id}
              className={tab === id ? "active" : ""}
              onClick={() => setTab(id)}
            >
              {label}
            </button>
          ))}
        </nav>
        <div className="settings-options">
          {tab === "general" && (
            <>
              <h2>{tr("일반")}</h2>
              <label>
                {tr(" 언어 ")}
                {select("language", [
                  [tr("한국어"), "ko"],
                  ["English", "en"],
                ])}
              </label>
              <label>
                {tr(" 한 주의 시작 ")}
                {select("weekStartsOn", [
                  [tr("일요일"), 0],
                  [tr("월요일"), 1],
                ])}
              </label>
              <label>
                {tr(" 날짜 형식 ")}
                {select("dateFormat", [
                  ["2026-11-12", "yyyy-mm-dd"],
                  ["11/12/2026", "mm/dd/yyyy"],
                  ["12/11/2026", "dd/mm/yyyy"],
                ])}
              </label>
              <label>
                {tr(" 시간 형식 ")}
                {select("timeFormat", [
                  [tr("24시간"), "24h"],
                  [tr("12시간"), "12h"],
                ])}
              </label>
              <label>
                {tr(" 시작 화면 ")}
                {select("defaultView", [
                  [tr("오늘"), "today"],
                  [tr("받은 편지함"), "inbox"],
                  [tr("전체"), "all"],
                ])}
              </label>
            </>
          )}
          {tab === "appearance" && (
            <>
              <h2>{tr("화면")}</h2>
              <label>
                {tr(" 테마 ")}
                {select("theme", [
                  [tr("시스템"), "system"],
                  [tr("라이트"), "light"],
                  [tr("다크"), "dark"],
                ])}
              </label>
              <label>
                {tr(" 강조 색상 ")}
                <input
                  type="color"
                  value={value.accent}
                  onChange={(e) => update("accent", e.target.value)}
                />
              </label>
              <label>
                {tr(" 화면 밀도 ")}
                {select("density", [
                  [tr("기본"), "comfortable"],
                  [tr("압축"), "compact"],
                ])}
              </label>
            </>
          )}
          {tab === "tasks" && (
            <>
              <h2>{tr("할 일")}</h2>
              <label>
                {tr(" 기본 우선순위 ")}
                {select("defaultPriority", [
                  [tr("없음"), 0],
                  [tr("낮음"), 1],
                  [tr("보통"), 2],
                  [tr("높음"), 3],
                ])}
              </label>
              <label>
                {tr(" 새 할 일 기본 날짜 ")}
                {select("defaultDue", [
                  [tr("없음"), "none"],
                  [tr("오늘"), "today"],
                  [tr("내일"), "tomorrow"],
                ])}
              </label>
              <label>
                {tr("완료한 할 일 표시")}
                {toggle("showCompleted")}
              </label>
              <label>
                {tr("삭제 전 확인")}
                {toggle("confirmDelete")}
              </label>
            </>
          )}
          {tab === "focus" && (
            <>
              <h2>{tr("집중")}</h2>
              <label>
                {tr("집중 시간(분)")}
                {number("focusMinutes", 1, 180)}
              </label>
              <label>
                {tr("짧은 휴식(분)")}
                {number("shortBreakMinutes", 1, 60)}
              </label>
              <label>
                {tr("긴 휴식(분)")}
                {number("longBreakMinutes", 1, 120)}
              </label>
              <label>
                {tr("종료 알림음")}
                {toggle("focusSound")}
              </label>
            </>
          )}
          {tab === "notifications" && (
            <>
              <h2>{tr("알림")}</h2>
              <label>
                {tr("데스크톱 알림")}
                {toggle("desktopNotifications")}
              </label>
              <button className="primary" onClick={permission}>
                {tr(" 알림 권한 요청 ")}
              </button>
              <p className="settings-note">
                {tr(
                  " 앱이 실행 중일 때 예약한 시간에 알림을 표시합니다. 앱을 종료한 동안에는 알림이 전달되지 않습니다. ",
                )}
              </p>
            </>
          )}
          {tab === "desktop" && (
            <>
              <h2>{tr("데스크톱")}</h2>
              <label>
                {tr(" 시작 시 최소화 ")}
                <input
                  type="checkbox"
                  disabled={!native}
                  checked={value.startMinimized}
                  onChange={(e) => update("startMinimized", e.target.checked)}
                />
              </label>
              <label>
                {tr(" 시스템 시작 시 실행 ")}
                <input
                  type="checkbox"
                  disabled={!native}
                  checked={value.autoLaunch}
                  onChange={async (e) => {
                    const enabled = e.target.checked;
                    try {
                      const a = await import("@tauri-apps/plugin-autostart");
                      if (enabled) await a.enable();
                      else await a.disable();
                      update("autoLaunch", enabled);
                    } catch (error) {
                      setNativeError(String(error));
                    }
                  }}
                />
              </label>
              <p className="settings-note">
                {tr(
                  " Windows 설치 앱에서 시스템 시작 실행과 최소화 시작을 지원합니다. 브라우저에서는 사용할 수 없습니다. ",
                )}
              </p>
            </>
          )}
          {nativeError && <p className="error">{nativeError}</p>}
          <div className="settings-footer">
            <button onClick={() => onChange({ ...defaults })}>
              {tr(" 기본값 복원 ")}
            </button>
            <span>{tr("설정은 이 기기에 자동 저장됩니다.")}</span>
          </div>
        </div>
      </div>
    </section>
  );
}
