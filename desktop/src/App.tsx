import { tr, setLanguage } from "./i18n";
import {
  Settings,
  loadPreferences,
  savePreferences,
  applyPreferences,
  type Preferences,
} from "./Settings";
import { Tags } from "./Tags";
import { Habits, Matrix, Statistics } from "./ExtraViews";
import { initializeApp, getApps } from "firebase/app";
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  onAuthStateChanged,
  type User,
} from "firebase/auth";
import {
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager,
} from "firebase/firestore";
import { useEffect, useMemo, useState } from "react";
import {
  CalendarDays,
  Check,
  Circle,
  Clock3,
  Flag,
  Hash,
  Inbox,
  LayoutGrid,
  ListTodo,
  Menu,
  Plus,
  Search,
  Settings2,
  Sun,
  Tag,
  Timer,
  Trash2,
  X,
} from "lucide-react";
import "./style.css";
import { useCollection } from "./useCollection";
import { Focus } from "./Focus";
import { Calendar } from "./Calendar";
import { Countdowns, Markdown, Timeline, type Countdown } from "./MoreViews";
import {
  dateKey,
  taskDate,
  nextOccurrence,
  parseQuickAdd,
  validateBackup,
  type Task,
  type TaskList,
  type Tag as TaskTag,
  type Habit,
  type HabitLog,
  type FocusSession,
  type SmartFilter,
} from "./domain";

type View =
  | "inbox"
  | "today"
  | "upcoming"
  | "all"
  | "completed"
  | "calendar"
  | "focus"
  | "board"
  | string;
const config = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};
const qaMode = import.meta.env.VITE_QA_MODE === "true";
const configured = Object.values(config).every(Boolean);
const app = configured ? (getApps()[0] ?? initializeApp(config)) : null;
const auth = app ? getAuth(app) : null;
const db = app
  ? initializeFirestore(app, {
      localCache: persistentLocalCache({
        tabManager: persistentMultipleTabManager(),
      }),
    })
  : null;
const uid = () => crypto.randomUUID();
function App() {
  const [user, setUser] = useState<User | null>(
    qaMode || !configured
      ? ({ uid: "local", displayName: tr("로컬 사용자"), email: "" } as User)
      : null,
  );
  const [error, setError] = useState("");
  const dataDb = !user || qaMode || user.uid === "local" ? null : db;
  const dataUid = user?.uid ?? "local";
  const taskStore = useCollection<Task>(dataDb, dataUid, "tasks", setError);
  const { items: tasks, setItems: setTasks, put: write } = taskStore;
  const listStore = useCollection<TaskList>(dataDb, dataUid, "lists", setError);
  const { items: lists } = listStore;
  const tagStore = useCollection<TaskTag>(dataDb, dataUid, "tags", setError);
  const { items: tags } = tagStore;
  const habitStore = useCollection<Habit>(dataDb, dataUid, "habits", setError);
  const logStore = useCollection<HabitLog>(
    dataDb,
    dataUid,
    "habitLogs",
    setError,
  );
  const sessionStore = useCollection<FocusSession>(
    dataDb,
    dataUid,
    "sessions",
    setError,
  );
  const filterStore = useCollection<SmartFilter>(
    dataDb,
    dataUid,
    "filters",
    setError,
  );
  const countdownStore = useCollection<Countdown>(
    dataDb,
    dataUid,
    "countdowns",
    setError,
  );
  const [previewNotes, setPreviewNotes] = useState(false);
  const [sort, setSort] = useState("priority");
  const [group, setGroup] = useState("none");
  const [checked, setChecked] = useState<string[]>([]);
  const [tagFilter, setTagFilter] = useState("");
  const today = dateKey(new Date());
  const formatDate = (date: Date) => {
    const key = dateKey(date);
    return prefs.dateFormat === "mm/dd/yyyy"
      ? key.slice(5, 7) + "/" + key.slice(8) + "/" + key.slice(0, 4)
      : prefs.dateFormat === "dd/mm/yyyy"
        ? key.slice(8) + "/" + key.slice(5, 7) + "/" + key.slice(0, 4)
        : key;
  };
  const [prefs, setPrefs] = useState<Preferences>(loadPreferences);
  setLanguage(prefs.language);
  const [view, setView] = useState<View>(() => loadPreferences().defaultView);
  const [selected, setSelected] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [draft, setDraft] = useState("");
  const [sidebar, setSidebar] = useState(true);
  const [showCompleted, setShowCompleted] = useState(
    () => loadPreferences().showCompleted,
  );
  const [subtaskDraft, setSubtaskDraft] = useState("");
  useEffect(() => {
    savePreferences(prefs);
    applyPreferences(prefs);
    const media = matchMedia("(prefers-color-scheme: dark)");
    const handler = () => applyPreferences(prefs);
    media.addEventListener("change", handler);
    return () => media.removeEventListener("change", handler);
  }, [prefs]);
  useEffect(() => {
    if (loadPreferences().startMinimized && "__TAURI_INTERNALS__" in window)
      import("@tauri-apps/api/window")
        .then((w) => w.getCurrentWindow().hide())
        .catch((e) => setError(String(e)));
  }, []);
  useEffect(
    () =>
      qaMode ? undefined : auth ? onAuthStateChanged(auth, setUser) : undefined,
    [],
  );
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        document.getElementById("search")?.focus();
      }
      if ((e.ctrlKey || e.metaKey) && e.key === "n") {
        e.preventDefault();
        setView("inbox");
        setTimeout(() => document.getElementById("quick-add")?.focus(), 0);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);
  useEffect(() => {
    if (!("__TAURI_INTERNALS__" in window)) return;
    let disposed = false;
    let release: (() => Promise<void>) | undefined;
    import("@tauri-apps/plugin-global-shortcut")
      .then(async (shortcut) => {
        if (disposed) return;
        await shortcut.register("CommandOrControl+Shift+A", async (event) => {
          if (event.state !== "Pressed") return;
          const { getCurrentWindow } = await import("@tauri-apps/api/window");
          await getCurrentWindow().show();
          await getCurrentWindow().setFocus();
          setView("inbox");
          setTimeout(() => document.getElementById("quick-add")?.focus(), 0);
        });
        release = () => shortcut.unregister("CommandOrControl+Shift+A");
        if (disposed) await release();
      })
      .catch((e) => setError(String(e)));
    return () => {
      disposed = true;
      void release?.();
    };
  }, []);
  const notify = async (message: string) => {
    setError(message);
    if (!prefs.desktopNotifications) return;
    try {
      if ("__TAURI_INTERNALS__" in window) {
        const n = await import("@tauri-apps/plugin-notification");
        if (await n.isPermissionGranted())
          n.sendNotification({ title: "BetterTick", body: message });
      } else if (
        "Notification" in window &&
        Notification.permission === "granted"
      )
        new Notification("BetterTick", { body: message });
    } catch (e) {
      setError(String(e));
    }
  };
  useEffect(() => {
    const check = () => {
      const now = Date.now();
      for (const t of tasks) {
        if (t.deletedAt || t.isCompleted || !taskDate(t)) continue;
        for (const minutes of t.reminderMinutes ?? []) {
          const date = new Date(
            dateKey(taskDate(t)!) + "T" + (t.dueTime || "09:00"),
          );
          const when = date.getTime() - minutes * 60000;
          const key =
            "bettertick.reminded." + dataUid + "." + t.id + "." + when;
          if (
            now >= when &&
            now - when < 86400000 &&
            !localStorage.getItem(key)
          ) {
            localStorage.setItem(key, "1");
            void notify(t.title);
          }
        }
      }
    };
    check();
    const id = setInterval(check, 15000);
    return () => clearInterval(id);
  }, [tasks, prefs.desktopNotifications, dataUid]);
  const addTask = async () => {
    if (!draft.trim() || !user) return;
    const parsed = parseQuickAdd(draft);
    if (!parsed.title) {
      setError(tr("할 일 제목을 입력하세요."));
      return;
    }
    const id = uid();
    const tagIds: string[] = [];
    for (const name of parsed.tagNames) {
      let tag = tags.find((t) => t.name === name);
      if (!tag) {
        tag = { id: uid(), name, color: prefs.accent };
        await tagStore.put(tag);
      }
      tagIds.push(tag.id);
    }
    const task: Task = {
      id,
      title: parsed.title,
      notes: "",
      listId: lists.find((l) => l.id === view)?.id ?? "",
      isCompleted: false,
      isNote: view === "notes",
      priority: parsed.priority ?? prefs.defaultPriority,
      dueDate:
        parsed.dueDate ??
        (view === "upcoming"
          ? new Date(Date.now() + 86400000).toISOString()
          : view === "today" || prefs.defaultDue === "today"
            ? new Date().toISOString()
            : prefs.defaultDue === "tomorrow"
              ? new Date(Date.now() + 86400000).toISOString()
              : null),
      tagIds,
      createdAt: new Date().toISOString(),
      sortOrder: Date.now(),
    };
    setDraft("");
    await write(task);
    setSelected(id);
  };
  const addList = async () => {
    const name = prompt(tr("새 목록 이름"));
    if (!name?.trim()) return;
    const id = uid();
    await listStore.put({
      id,
      name: name.trim(),
      color: prefs.accent,
      sortOrder: lists.length,
    });
    setView(id);
  };
  const completeTask = async (t: Task) => {
    const completed = !t.isCompleted;
    await write({
      ...t,
      isCompleted: completed,
      completedAt: completed ? new Date().toISOString() : null,
    });
    if (completed) {
      const next = nextOccurrence(t);
      if (next && !tasks.some((x) => x.id === next.id)) await write(next);
    }
  };
  const removeTask = async (id: string) => {
    if (prefs.confirmDelete && !confirm(tr("할 일을 휴지통으로 이동할까요?")))
      return;
    const t = tasks.find((t) => t.id === id);
    if (t) await write({ ...t, deletedAt: new Date().toISOString() });
    setSelected(null);
  };
  const deleteList = async (l: TaskList) => {
    if (!confirm(tr("목록을 삭제하고 할 일을 받은 편지함으로 이동할까요?")))
      return;
    for (const t of tasks.filter((t) => t.listId === l.id))
      await write({ ...t, listId: "" });
    await listStore.remove(l.id);
    setView("inbox");
  };
  const batch = async (action: string) => {
    for (const t of tasks.filter((t) => checked.includes(t.id))) {
      if (action === "delete")
        await write({ ...t, deletedAt: new Date().toISOString() });
      else if (action === "complete") await completeTask(t);
      else if (action.startsWith("priority:"))
        await write({ ...t, priority: Number(action.split(":")[1]) });
      else await write({ ...t, listId: action });
    }
    setChecked([]);
  };
  const visible = useMemo(
    () =>
      tasks
        .filter((t) => {
          if (view === "trash") return !!t.deletedAt;
          if (t.deletedAt) return false;
          if (tagFilter && !t.tagIds?.includes(tagFilter)) return false;
          const filter = filterStore.items.find((f) => f.id === view);
          if (
            filter &&
            ((filter.priority >= 0 && filter.priority !== t.priority) ||
              (filter.tagId && !t.tagIds?.includes(filter.tagId)) ||
              (filter.listId && t.listId !== filter.listId) ||
              (filter.dated && !taskDate(t)))
          )
            return false;
          if (
            search &&
            !(t.title + " " + (t.notes ?? ""))
              .toLowerCase()
              .includes(search.toLowerCase())
          )
            return false;
          if (view === "notes") return !!t.isNote;
          if (view === "completed") return t.isCompleted;
          if (!showCompleted && t.isCompleted) return false;
          const due = taskDate(t);
          const key = due ? dateKey(due) : "";
          if (view === "today") return !!due && key <= dateKey(new Date());
          if (view === "upcoming") return !!due && key > dateKey(new Date());
          if (view === "inbox") return !t.listId;
          if (filter) return true;
          if (
            view === "all" ||
            view === "calendar" ||
            view === "focus" ||
            view === "board" ||
            view === "timeline"
          )
            return true;
          return t.listId === view;
        })
        .sort(
          (a, b) =>
            (group === "list"
              ? (a.listId ?? "").localeCompare(b.listId ?? "")
              : group === "section"
                ? (a.section ?? "").localeCompare(b.section ?? "")
                : group === "date"
                  ? (taskDate(a)
                      ? dateKey(taskDate(a)!)
                      : "9999"
                    ).localeCompare(
                      taskDate(b) ? dateKey(taskDate(b)!) : "9999",
                    )
                  : 0) ||
            (a.isCompleted === b.isCompleted ? 0 : a.isCompleted ? 1 : -1) ||
            Number(!!b.pinned) - Number(!!a.pinned) ||
            (sort === "title"
              ? a.title.localeCompare(b.title)
              : sort === "date"
                ? (taskDate(a)?.getTime() ?? Infinity) -
                  (taskDate(b)?.getTime() ?? Infinity)
                : sort === "manual"
                  ? 0
                  : b.priority - a.priority) ||
            (a.sortOrder ?? 0) - (b.sortOrder ?? 0),
        ),
    [
      tasks,
      view,
      search,
      showCompleted,
      tagFilter,
      filterStore.items,
      sort,
      group,
    ],
  );
  const exportData = () => {
    const payload = {
      format: "bettertick-backup",
      version: 1,
      exportedAt: new Date().toISOString(),
      tasks,
      lists,
      tags,
      habits: habitStore.items,
      habitLogs: logStore.items,
      sessions: sessionStore.items,
      filters: filterStore.items,
      countdowns: countdownStore.items,
      preferences: prefs,
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "bettertick-backup-" + dateKey(new Date()) + ".json";
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  const importData = async (file: File) => {
    try {
      if (file.size > 10_000_000)
        throw Error(tr("백업 파일은 10MB 이하여야 합니다."));
      const parsed = validateBackup(JSON.parse(await file.text()));
      if (
        !confirm(tr("백업 데이터를 가져올까요? 동일한 ID의 항목은 덮어씁니다."))
      )
        return;
      for (const t of parsed.tasks) await write(t);
      for (const l of parsed.lists) await listStore.put(l);
      for (const t of parsed.tags ?? []) await tagStore.put(t);
      for (const h of parsed.habits ?? []) await habitStore.put(h);
      for (const l of parsed.habitLogs ?? []) await logStore.put(l);
      for (const s of parsed.sessions ?? []) await sessionStore.put(s);
      for (const f of parsed.filters ?? []) await filterStore.put(f);
      for (const c of parsed.countdowns ?? []) await countdownStore.put(c);
      if (parsed.preferences && typeof parsed.preferences === "object") {
        const next = loadPreferences(parsed.preferences);
        setPrefs(next);
        setShowCompleted(next.showCompleted);
      }
      setError(tr("백업을 가져왔습니다."));
    } catch (e) {
      setError(tr("가져오기 실패: ") + String(e));
    }
  };
  const active = tasks.find((t) => t.id === selected);
  const nav = [
    { id: "today", name: tr("오늘"), icon: Sun },
    { id: "upcoming", name: tr("예정"), icon: CalendarDays },
    { id: "inbox", name: tr("받은 편지함"), icon: Inbox },
    { id: "all", name: tr("전체"), icon: ListTodo },
    { id: "completed", name: tr("완료"), icon: Check },
    { id: "trash", name: tr("휴지통"), icon: Trash2 },
  ];
  if (!user)
    return (
      <div className="center">
        <div className="login-card">
          <div className="logo-mark">✓</div>
          <h1>BetterTick</h1>
          <p>{tr("모바일과 PC에서 할 일을 함께 관리하세요.")}</p>
          <button
            className="primary"
            onClick={() =>
              setUser({ uid: "local", displayName: tr("로컬 사용자") } as User)
            }
          >
            {tr(" 로그인 없이 시작 ")}
          </button>
          <button
            className="primary"
            onClick={() =>
              auth &&
              signInWithPopup(auth, new GoogleAuthProvider()).catch((e) =>
                setError(e.message),
              )
            }
          >
            {tr(" Google 계정으로 로그인 ")}
          </button>
          {error && <p className="error">{error}</p>}
        </div>
      </div>
    );
  return (
    <div className="shell">
      {sidebar && (
        <aside className="sidebar">
          <div className="sidebar-header">
            <div className="brand">
              <div className="logo-mark">✓</div>
              <strong>BetterTick</strong>
              <button
                className="icon-button push"
                title={tr("사이드바 닫기")}
                onClick={() => setSidebar(false)}
              >
                <Menu size={17} />
              </button>
            </div>
            <div className="profile">
              <div className="avatar">
                {(user.displayName ?? "U").slice(0, 1)}
              </div>
              <span>{user.displayName ?? user.email}</span>
              <button
                className="icon-button"
                title={tr("설정")}
                onClick={() => setView("settings")}
              >
                <Settings2 size={16} />
              </button>
            </div>
          </div>
          <div className="nav-section">
            {nav.map((item) => (
              <button
                key={item.id}
                className={"nav-item " + (view === item.id ? "active" : "")}
                onClick={() => {
                  setView(item.id);
                  setSelected(null);
                  setTagFilter("");
                  setChecked([]);
                }}
              >
                <item.icon size={18} />
                <span>{item.name}</span>
                <span className="count">
                  {item.id === "today"
                    ? tasks.filter(
                        (t) =>
                          !t.deletedAt &&
                          !t.isCompleted &&
                          taskDate(t) &&
                          dateKey(taskDate(t)!) <= today,
                      ).length
                    : ""}
                </span>
              </button>
            ))}
          </div>
          <div className="section-heading">
            {tr(" 내 목록")}{" "}
            <button
              className="icon-button"
              onClick={addList}
              title={tr("목록 추가")}
            >
              <Plus size={17} />
            </button>
          </div>
          <div className="nav-section">
            {lists
              .filter((l) => !l.isArchived)
              .slice()
              .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
              .map((l) => (
                <button
                  key={l.id}
                  className={"nav-item " + (view === l.id ? "active" : "")}
                  onClick={() => {
                    setView(l.id);
                    setSelected(null);
                    setTagFilter("");
                    setChecked([]);
                  }}
                >
                  <span
                    className="list-dot"
                    style={{ background: l.color || "#4776ef" }}
                  />
                  <span>
                    {l.folder ? l.folder + " / " : ""}
                    {l.name}
                  </span>
                  <span className="count">
                    {
                      tasks.filter(
                        (t) =>
                          !t.deletedAt && t.listId === l.id && !t.isCompleted,
                      ).length
                    }
                  </span>
                </button>
              ))}
          </div>
          <div className="section-heading">
            {tr(" 스마트 필터 ")}
            <button
              title={tr("필터 추가")}
              onClick={() => {
                const name = prompt(tr("필터 이름"));
                if (!name?.trim()) return;
                filterStore.put({
                  id: uid(),
                  name: name.trim(),
                  priority: -1,
                  tagId: tagFilter,
                  listId: lists.some((l) => l.id === view) ? view : "",
                  dated: false,
                });
              }}
            >
              <Plus size={16} />
            </button>
          </div>
          <div className="nav-section">
            {filterStore.items.map((f) => (
              <button
                key={f.id}
                className={"nav-item " + (view === f.id ? "active" : "")}
                onClick={() => {
                  setView(f.id);
                  setTagFilter("");
                }}
              >
                <Flag size={16} />
                <span>{f.name}</span>
              </button>
            ))}
          </div>
          <div className="sidebar-bottom">
            <button className="nav-item" onClick={() => setView("notes")}>
              <ListTodo size={18} />
              {tr(" 노트 ")}
            </button>
            <button className="nav-item" onClick={() => setView("timeline")}>
              <Clock3 size={18} />
              {tr(" 타임라인 ")}
            </button>
            <button className="nav-item" onClick={() => setView("countdowns")}>
              <CalendarDays size={18} />
              {tr(" 디데이 ")}
            </button>
            <button className="nav-item" onClick={() => setView("statistics")}>
              <LayoutGrid size={18} />
              {tr(" 통계 ")}
            </button>
            <button
              className={"nav-item " + (view === "calendar" ? "active" : "")}
              onClick={() => setView("calendar")}
            >
              <CalendarDays size={18} />
              {tr(" 캘린더 ")}
            </button>
            <button
              className={"nav-item " + (view === "tags" ? "active" : "")}
              onClick={() => setView("tags")}
            >
              <Tag size={18} />
              {tr(" 태그 ")}
            </button>
            <button
              className={"nav-item " + (view === "habits" ? "active" : "")}
              onClick={() => setView("habits")}
            >
              <Check size={18} />
              {tr(" 습관 ")}
            </button>
            <button
              className={"nav-item " + (view === "matrix" ? "active" : "")}
              onClick={() => setView("matrix")}
            >
              <LayoutGrid size={18} />
              {tr(" 아이젠하워 ")}
            </button>
            <button
              className={"nav-item " + (view === "board" ? "active" : "")}
              onClick={() => setView("board")}
            >
              <LayoutGrid size={18} />
              {tr(" 칸반 보드 ")}
            </button>
            <button
              className={"nav-item " + (view === "focus" ? "active" : "")}
              onClick={() => setView("focus")}
            >
              <Timer size={18} />
              {tr(" 집중 ")}
            </button>
          </div>
        </aside>
      )}
      <main className="main">
        <header className="toolbar">
          {!sidebar && (
            <button className="icon-button" onClick={() => setSidebar(true)}>
              <Menu size={19} />
            </button>
          )}
          <div className="search-box">
            <Search size={17} />
            <input
              id="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={tr("할 일 검색 (Ctrl+K)")}
            />
          </div>
          <button
            className="icon-button"
            title={tr("백업 내보내기")}
            onClick={exportData}
          >
            {tr(" 내보내기 ")}
          </button>
          <label className="backup-import">
            {tr(" 가져오기 ")}
            <input
              type="file"
              accept=".json,application/json"
              style={{ display: "none" }}
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) void importData(file);
                e.target.value = "";
              }}
            />
          </label>
          <button
            className="icon-button"
            title={tr("오늘")}
            onClick={() => {
              setView("today");
              setSelected(null);
            }}
          >
            <Sun size={19} />
          </button>
        </header>
        <div className="focus-container" hidden={view !== "focus"}>
          <Focus
            key={dataUid}
            storageKey={"bettertick.desktop.timer.v1." + dataUid}
            prefs={prefs}
            tasks={tasks}
            sessions={sessionStore.items}
            onSession={sessionStore.put}
            onNotify={(message) => {
              void notify(message);
            }}
          />
        </div>
        {view === "focus" ? null : view === "settings" ? (
          <Settings
            value={prefs}
            onChange={(p) => {
              setPrefs(p);
              setShowCompleted(p.showCompleted);
            }}
            onClose={() => setView(prefs.defaultView)}
          />
        ) : view === "tags" ? (
          <Tags
            tags={tags}
            onSave={tagStore.put}
            onFilter={(id) => {
              setTagFilter(id);
              setView("all");
            }}
            onDelete={(id) => {
              tagStore.remove(id);
              tasks
                .filter((t) => t.tagIds?.includes(id))
                .forEach((t) =>
                  write({ ...t, tagIds: t.tagIds?.filter((x) => x !== id) }),
                );
            }}
          />
        ) : view === "habits" ? (
          <Habits
            habits={habitStore.items}
            logs={logStore.items}
            onSave={habitStore.put}
            onLog={(l) =>
              logStore.put({
                ...l,
                isCompleted:
                  l.value >=
                  (habitStore.items.find((h) => h.id === l.habitId)?.target ??
                    1),
                completedAt:
                  l.value >=
                  (habitStore.items.find((h) => h.id === l.habitId)?.target ??
                    1)
                    ? new Date().toISOString()
                    : null,
              })
            }
            onDelete={(id) => {
              habitStore.remove(id);
              logStore.items
                .filter((l) => l.habitId === id)
                .forEach((l) => logStore.remove(l.id));
            }}
          />
        ) : view === "countdowns" ? (
          <Countdowns
            items={countdownStore.items}
            onSave={countdownStore.put}
            onDelete={countdownStore.remove}
          />
        ) : view === "timeline" ? (
          <Timeline tasks={visible} onSave={write} onSelect={setSelected} />
        ) : view === "statistics" ? (
          <Statistics
            tasks={tasks}
            habits={habitStore.items}
            logs={logStore.items}
            sessions={sessionStore.items}
          />
        ) : view === "matrix" ? (
          <Matrix
            tasks={tasks}
            onMove={write}
            onSelect={(id) => {
              setSelected(id);
              setView("all");
            }}
          />
        ) : view === "calendar" ? (
          <Calendar
            tasks={tasks}
            prefs={prefs}
            onSave={write}
            onSelect={(id) => setSelected(id)}
            onAdd={(date) => {
              const title = prompt(tr("할 일 제목"));
              if (title?.trim()) {
                const id = uid();
                write({
                  id,
                  title: title.trim(),
                  dueDate: date + "T12:00:00",
                  priority: prefs.defaultPriority,
                  listId: "",
                  isCompleted: false,
                  sortOrder: Date.now(),
                });
                setSelected(id);
              }
            }}
          />
        ) : view === "board" ? (
          <section className="content">
            <div className="heading">
              <h1>{tr("칸반 보드")}</h1>
            </div>
            <div className="board">
              {[0, 1, 2, 3].map((p) => (
                <div
                  className="board-column"
                  key={p}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    const t = tasks.find(
                      (t) => t.id === e.dataTransfer.getData("text/plain"),
                    );
                    if (t) write({ ...t, priority: p });
                  }}
                >
                  <h3>
                    {
                      [tr("우선순위 없음"), tr("낮음"), tr("보통"), tr("높음")][
                        p
                      ]
                    }{" "}
                    <span>
                      {visible.filter((t) => t.priority === p).length}
                    </span>
                  </h3>
                  {visible
                    .filter((t) => t.priority === p)
                    .map((t) => (
                      <button
                        className="board-card"
                        draggable
                        onDragStart={(e) =>
                          e.dataTransfer.setData("text/plain", t.id)
                        }
                        key={t.id}
                        onClick={() => setSelected(t.id)}
                      >
                        {t.title}
                      </button>
                    ))}
                </div>
              ))}
            </div>
          </section>
        ) : (
          <section className="content">
            <div className="heading">
              <div>
                <h1>
                  {[...nav, { id: "board", name: tr("칸반 보드") }].find(
                    (n) => n.id === view,
                  )?.name ??
                    lists.find((l) => l.id === view)?.name ??
                    filterStore.items.find((f) => f.id === view)?.name ??
                    (view === "notes" ? tr("노트") : tr("할 일"))}
                </h1>
                <p>
                  {visible.length}
                  {tr("개의 할 일")}
                </p>
              </div>
              <button
                className="subtle"
                onClick={() => setShowCompleted(!showCompleted)}
              >
                {showCompleted ? tr("완료 숨기기") : tr("완료 표시")}
              </button>
            </div>
            <div className="view-controls">
              <label>
                {tr(" 정렬 ")}
                <select
                  aria-label={tr("정렬")}
                  value={sort}
                  onChange={(e) => setSort(e.target.value)}
                >
                  <option value="priority">{tr("우선순위")}</option>
                  <option value="date">{tr("날짜")}</option>
                  <option value="title">{tr("제목")}</option>
                  <option value="manual">{tr("직접 정렬")}</option>
                </select>
              </label>
              <label>
                {tr(" 그룹 ")}
                <select
                  aria-label={tr("그룹")}
                  value={group}
                  onChange={(e) => setGroup(e.target.value)}
                >
                  <option value="none">{tr("없음")}</option>
                  <option value="list">{tr("목록")}</option>
                  <option value="date">{tr("날짜")}</option>
                  <option value="section">{tr("섹션")}</option>
                </select>
              </label>
              <select
                aria-label={tr("태그 필터")}
                value={tagFilter}
                onChange={(e) => setTagFilter(e.target.value)}
              >
                <option value="">{tr("모든 태그")}</option>
                {tags.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
              {lists.some((l) => l.id === view) && (
                <>
                  <button
                    onClick={() => {
                      const l = lists.find((l) => l.id === view)!;
                      const name = prompt(tr("목록 이름"), l.name);
                      if (name?.trim())
                        listStore.put({ ...l, name: name.trim() });
                    }}
                  >
                    {tr(" 목록 이름 변경 ")}
                  </button>
                  <button
                    onClick={() => {
                      const l = lists.find((l) => l.id === view)!;
                      const folder = prompt(
                        tr("폴더 이름 (비우면 폴더 해제)"),
                        l.folder ?? "",
                      );
                      if (folder !== null)
                        listStore.put({ ...l, folder: folder.trim() });
                    }}
                  >
                    {tr(" 폴더 이동 ")}
                  </button>
                  <input
                    aria-label={tr("목록 색상")}
                    type="color"
                    value={lists.find((l) => l.id === view)!.color}
                    onChange={(e) =>
                      listStore.put({
                        ...lists.find((l) => l.id === view)!,
                        color: e.target.value,
                      })
                    }
                  />
                  <button
                    className="danger"
                    onClick={() =>
                      deleteList(lists.find((l) => l.id === view)!)
                    }
                  >
                    {tr(" 목록 삭제 ")}
                  </button>
                </>
              )}
              {filterStore.items.some((f) => f.id === view) && (
                <>
                  <select
                    aria-label={tr("필터 우선순위")}
                    value={
                      filterStore.items.find((f) => f.id === view)!.priority
                    }
                    onChange={(e) =>
                      filterStore.put({
                        ...filterStore.items.find((f) => f.id === view)!,
                        priority: Number(e.target.value),
                      })
                    }
                  >
                    <option value="-1">{tr("모든 우선순위")}</option>
                    {[0, 1, 2, 3].map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                  <button
                    className="danger"
                    onClick={() => {
                      filterStore.remove(view);
                      setView("all");
                    }}
                  >
                    {tr(" 필터 삭제 ")}
                  </button>
                </>
              )}
            </div>
            {checked.length > 0 && (
              <div className="batch-bar">
                <strong>
                  {checked.length}
                  {tr("개 선택")}
                </strong>
                <button onClick={() => batch("complete")}>
                  {tr(" 선택 완료 전환 ")}
                </button>
                <button onClick={() => batch("delete")}>
                  {tr("선택 삭제")}
                </button>
                <select
                  aria-label={tr("선택 목록 이동")}
                  value="_"
                  onChange={(e) => batch(e.target.value)}
                >
                  <option value="_" disabled>
                    {tr(" 목록 이동 ")}
                  </option>
                  <option value="">{tr("받은 편지함")}</option>
                  {lists.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name}
                    </option>
                  ))}
                </select>
                <button onClick={() => setChecked([])}>
                  {tr("선택 해제")}
                </button>
              </div>
            )}
            <form
              className="add-task"
              onSubmit={(e) => {
                e.preventDefault();
                void addTask();
              }}
            >
              <Plus size={19} />
              <input
                id="quick-add"
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder={tr("할 일 추가 · 내일 #태그 !3 (Ctrl+N)")}
              />
              <button type="submit" disabled={!draft.trim()}>
                {tr(" 추가 ")}
              </button>
            </form>
            <div className="tasks">
              {visible.length === 0 ? (
                <div className="empty">
                  <Circle size={35} />
                  <h3>{tr("할 일이 없어요")}</h3>
                  <p>{tr("새로운 할 일을 추가해 보세요.")}</p>
                </div>
              ) : (
                visible.map((t) => (
                  <div
                    className={"task " + (selected === t.id ? "selected" : "")}
                    key={t.id}
                    draggable
                    onDragStart={(e) =>
                      e.dataTransfer.setData("text/plain", t.id)
                    }
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => {
                      e.preventDefault();
                      const dragged = tasks.find(
                        (x) => x.id === e.dataTransfer.getData("text/plain"),
                      );
                      if (dragged) {
                        setSort("manual");
                        const before = visible[visible.indexOf(t) - 1];
                        write({
                          ...dragged,
                          sortOrder:
                            ((t.sortOrder ?? 0) +
                              (before?.sortOrder ??
                                (t.sortOrder ?? 0) - 2000)) /
                            2,
                        });
                      }
                    }}
                  >
                    <input
                      type="checkbox"
                      aria-label={tr("선택 ") + t.title}
                      checked={checked.includes(t.id)}
                      onChange={(e) =>
                        setChecked((ids) =>
                          e.target.checked
                            ? [...ids, t.id]
                            : ids.filter((id) => id !== t.id),
                        )
                      }
                    />
                    {group !== "none" && (
                      <small className="group-label">
                        {group === "list"
                          ? (lists.find((l) => l.id === t.listId)?.name ??
                            tr("받은 편지함"))
                          : group === "section"
                            ? t.section || tr("섹션 없음")
                            : taskDate(t)
                              ? dateKey(taskDate(t)!)
                              : tr("날짜 없음")}
                      </small>
                    )}
                    <button
                      className={"check " + (t.isCompleted ? "checked" : "")}
                      title={tr("완료 전환")}
                      onClick={() => completeTask(t)}
                    >
                      {t.isCompleted && <Check size={13} />}
                    </button>
                    <button
                      className="task-body"
                      onClick={() => setSelected(t.id)}
                    >
                      <span className={t.isCompleted ? "done" : ""}>
                        {t.title}
                      </span>
                      <small>
                        {taskDate(t)
                          ? formatDate(taskDate(t)!)
                          : tr("날짜 없음")}
                        {t.dueTime
                          ? " · " +
                            (prefs.timeFormat === "12h"
                              ? (Number(t.dueTime.slice(0, 2)) % 12 || 12) +
                                t.dueTime.slice(2) +
                                (Number(t.dueTime.slice(0, 2)) >= 12
                                  ? " PM"
                                  : " AM")
                              : t.dueTime)
                          : ""}
                        {t.notes ? tr(" · 메모") : ""}
                        {t.subtasks?.length
                          ? " · " +
                            t.subtasks.filter((s) => s.done).length +
                            "/" +
                            t.subtasks.length
                          : ""}
                      </small>
                    </button>
                    {t.deletedAt && (
                      <>
                        <button
                          onClick={() => write({ ...t, deletedAt: null })}
                        >
                          {tr(" 복원 ")}
                        </button>
                        <button
                          className="danger"
                          onClick={() => {
                            if (confirm(tr("영구 삭제할까요?"))) {
                              taskStore.remove(t.id);
                              setSelected(null);
                            }
                          }}
                        >
                          {tr(" 영구 삭제 ")}
                        </button>
                      </>
                    )}
                    {t.pinned && <span title={tr("고정됨")}>📌</span>}
                    {t.priority > 0 && (
                      <Flag size={15} className={"priority p" + t.priority} />
                    )}
                  </div>
                ))
              )}
            </div>
          </section>
        )}
      </main>
      {active && (
        <aside className="detail">
          <div className="detail-top">
            <span>{tr("할 일 상세")}</span>
            <button className="icon-button" onClick={() => setSelected(null)}>
              <X size={19} />
            </button>
          </div>
          <textarea
            className="detail-title"
            value={active.title}
            onChange={(e) =>
              setTasks((ts) =>
                ts.map((t) =>
                  t.id === active.id ? { ...t, title: e.target.value } : t,
                ),
              )
            }
            onBlur={(e) => write({ ...active, title: e.target.value })}
          />
          <label>
            <CalendarDays size={17} />
            {tr(" 날짜")}{" "}
            <input
              type="date"
              value={taskDate(active) ? dateKey(taskDate(active)!) : ""}
              onChange={(e) =>
                write({
                  ...active,
                  dueDate: e.target.value
                    ? new Date(e.target.value + "T12:00:00")
                    : null,
                })
              }
            />
          </label>
          <label>
            <Flag size={17} />
            {tr(" 우선순위")}{" "}
            <select
              value={active.priority}
              onChange={(e) =>
                write({ ...active, priority: Number(e.target.value) })
              }
            >
              {[
                ["0", tr("없음")],
                ["1", tr("낮음")],
                ["2", tr("보통")],
                ["3", tr("높음")],
              ].map(([v, n]) => (
                <option key={v} value={v}>
                  {n}
                </option>
              ))}
            </select>
          </label>
          <label>
            <Hash size={17} />
            {tr(" 목록")}{" "}
            <select
              value={active.listId || ""}
              onChange={(e) => write({ ...active, listId: e.target.value })}
            >
              <option value="">{tr("받은 편지함")}</option>
              {lists.map((l) => (
                <option value={l.id} key={l.id}>
                  {l.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            <Clock3 size={17} />
            {tr(" 반복")}{" "}
            <select
              value={active.repeatRule ?? ""}
              onChange={(e) =>
                write({ ...active, repeatRule: e.target.value || null })
              }
            >
              <option value="">{tr("반복 안 함")}</option>
              <option value="DAILY">{tr("매일")}</option>
              <option value="WEEKDAYS">{tr("평일")}</option>
              <option value="YEARLY">{tr("매년")}</option>
              <option value="WEEKLY">{tr("매주")}</option>
              <option value="MONTHLY">{tr("매월")}</option>
            </select>
          </label>
          <label>
            <Tag size={17} />
            {tr(" 태그")}{" "}
            <select
              value=""
              onChange={(e) => {
                if (e.target.value && !active.tagIds?.includes(e.target.value))
                  write({
                    ...active,
                    tagIds: [...(active.tagIds ?? []), e.target.value],
                  });
              }}
            >
              <option value="">{tr("태그 추가")}</option>
              {tags
                .filter((t) => !active.tagIds?.includes(t.id))
                .map((t) => (
                  <option value={t.id} key={t.id}>
                    {t.name}
                  </option>
                ))}
            </select>
          </label>
          <div className="task-tags">
            {(active.tagIds ?? []).map((id) => {
              const tag = tags.find((t) => t.id === id);
              return tag ? (
                <button
                  key={id}
                  onClick={() =>
                    write({
                      ...active,
                      tagIds: (active.tagIds ?? []).filter((x) => x !== id),
                    })
                  }
                  style={{ background: tag.color + "22", color: tag.color }}
                >
                  {tag.name} ×
                </button>
              ) : null;
            })}
          </div>
          <label>
            {tr(" 시작 날짜 ")}
            <input
              type="date"
              value={active.startDate ?? ""}
              onChange={(e) => write({ ...active, startDate: e.target.value })}
            />
          </label>
          <label>
            {tr(" 노트로 사용 ")}
            <input
              type="checkbox"
              checked={!!active.isNote}
              onChange={(e) => write({ ...active, isNote: e.target.checked })}
            />
          </label>
          <label>
            {tr(" 시간 ")}
            <input
              aria-label={tr("마감 시간")}
              type="time"
              value={active.dueTime ?? ""}
              onChange={(e) => write({ ...active, dueTime: e.target.value })}
            />
          </label>
          <label>
            {tr(" 소요 시간(분) ")}
            <input
              type="number"
              min="0"
              max="1440"
              value={active.duration ?? 0}
              onChange={(e) =>
                write({
                  ...active,
                  duration: Math.max(0, Math.min(1440, Number(e.target.value))),
                })
              }
            />
          </label>
          <label>
            {tr(" 섹션 ")}
            <input
              value={active.section ?? ""}
              onChange={(e) => write({ ...active, section: e.target.value })}
            />
          </label>
          <label>
            {tr(" 고정 ")}
            <input
              type="checkbox"
              checked={!!active.pinned}
              onChange={(e) => write({ ...active, pinned: e.target.checked })}
            />
          </label>
          <label>
            {tr(" 알림 ")}
            <select
              aria-label={tr("알림 추가")}
              value=""
              onChange={(e) => {
                if (
                  e.target.value !== "" &&
                  !(active.reminderMinutes ?? []).includes(
                    Number(e.target.value),
                  )
                )
                  write({
                    ...active,
                    reminderMinutes: [
                      ...(active.reminderMinutes ?? []),
                      Number(e.target.value),
                    ],
                  });
              }}
            >
              <option value="">{tr("알림 추가")}</option>
              <option value="0">{tr("정시")}</option>
              <option value="5">{tr("5분 전")}</option>
              <option value="30">{tr("30분 전")}</option>
              <option value="60">{tr("1시간 전")}</option>
              <option value="1440">{tr("1일 전")}</option>
            </select>
          </label>
          <div className="task-tags">
            {(active.reminderMinutes ?? []).map((m) => (
              <button
                key={m}
                onClick={() =>
                  write({
                    ...active,
                    reminderMinutes: active.reminderMinutes?.filter(
                      (x) => x !== m,
                    ),
                  })
                }
              >
                {m === 0 ? tr("정시") : m + tr("분 전")} ×
              </button>
            ))}
          </div>
          {active.repeatRule && (
            <>
              <label>
                {tr(" 반복 간격 ")}
                <input
                  type="number"
                  min="1"
                  max="365"
                  value={active.repeatInterval ?? 1}
                  onChange={(e) =>
                    write({
                      ...active,
                      repeatInterval: Math.max(
                        1,
                        Math.min(365, Number(e.target.value)),
                      ),
                    })
                  }
                />
              </label>
              <label>
                {tr(" 반복 종료 ")}
                <input
                  type="date"
                  value={active.repeatUntil ?? ""}
                  onChange={(e) =>
                    write({ ...active, repeatUntil: e.target.value })
                  }
                />
              </label>
              <label>
                {tr(" 완료일 기준 ")}
                <input
                  type="checkbox"
                  checked={!!active.repeatFromCompletion}
                  onChange={(e) =>
                    write({ ...active, repeatFromCompletion: e.target.checked })
                  }
                />
              </label>
            </>
          )}
          <button
            onClick={() => {
              const id = uid();
              write({
                ...active,
                id,
                title: active.title + tr(" (복사)"),
                isCompleted: false,
                completedAt: null,
                deletedAt: null,
                repeatSource: undefined,
                sortOrder: Date.now(),
              });
              setSelected(id);
            }}
          >
            {tr(" 할 일 복제 ")}
          </button>
          <div className="notes-label">
            {tr(" 하위 할 일 (")}
            {(active.subtasks ?? []).filter((s) => s.done).length}/
            {(active.subtasks ?? []).length})
          </div>
          <div className="subtask-list">
            {(active.subtasks ?? []).map((st) => (
              <div className="subtask" key={st.id}>
                <input
                  aria-label={tr("완료 ") + st.title}
                  type="checkbox"
                  checked={st.done}
                  onChange={() =>
                    write({
                      ...active,
                      subtasks: (active.subtasks ?? []).map((x) =>
                        x.id === st.id ? { ...x, done: !x.done } : x,
                      ),
                    })
                  }
                />
                <span
                  style={{ textDecoration: st.done ? "line-through" : "none" }}
                >
                  {st.title}
                </span>
                <button
                  aria-label={tr("삭제 ") + st.title}
                  onClick={() =>
                    write({
                      ...active,
                      subtasks: (active.subtasks ?? []).filter(
                        (x) => x.id !== st.id,
                      ),
                    })
                  }
                >
                  <X size={15} />
                </button>
              </div>
            ))}
          </div>
          <form
            className="subtask-add"
            onSubmit={(e) => {
              e.preventDefault();
              if (!subtaskDraft.trim()) return;
              void write({
                ...active,
                subtasks: [
                  ...(active.subtasks ?? []),
                  { id: uid(), title: subtaskDraft.trim(), done: false },
                ],
              });
              setSubtaskDraft("");
            }}
          >
            <input
              aria-label={tr("하위 할 일 입력")}
              placeholder={tr("하위 할 일 추가")}
              value={subtaskDraft}
              onChange={(e) => setSubtaskDraft(e.target.value)}
            />
            <button type="submit">{tr("추가")}</button>
          </form>
          <div className="notes-label">
            {tr(" 메모")}{" "}
            <button onClick={() => setPreviewNotes(!previewNotes)}>
              {previewNotes ? tr("편집") : tr("미리보기")}
            </button>
          </div>
          {previewNotes ? (
            <Markdown text={active.notes ?? ""} />
          ) : (
            <textarea
              className="notes"
              placeholder={tr("상세 내용을 입력하세요...")}
              value={active.notes ?? ""}
              onChange={(e) =>
                setTasks((ts) =>
                  ts.map((t) =>
                    t.id === active.id ? { ...t, notes: e.target.value } : t,
                  ),
                )
              }
              onBlur={(e) => write({ ...active, notes: e.target.value })}
            />
          )}
          <label className="attachment-add">
            {tr(" 첨부 파일 추가 (1MB) ")}
            <input
              aria-label={tr("첨부 파일 추가")}
              type="file"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                e.target.value = "";
                if (!file) return;
                if (file.size > 1000000) {
                  setError(tr("첨부 파일은 1MB 이하여야 합니다."));
                  return;
                }
                const reader = new FileReader();
                reader.onload = () =>
                  write({
                    ...active,
                    desktopAttachments: [
                      ...(active.desktopAttachments ?? []),
                      {
                        id: uid(),
                        name: file.name,
                        mime: file.type || "application/octet-stream",
                        data: String(reader.result),
                      },
                    ],
                  });
                reader.readAsDataURL(file);
              }}
            />
          </label>
          {(active.desktopAttachments ?? []).map((file) => (
            <div className="attachment" key={file.id}>
              <a href={file.data} download={file.name}>
                {file.name}
              </a>
              <button
                aria-label={file.name + tr(" 첨부 삭제")}
                onClick={() =>
                  write({
                    ...active,
                    desktopAttachments: active.desktopAttachments?.filter(
                      (a) => a.id !== file.id,
                    ),
                  })
                }
              >
                ×
              </button>
            </div>
          ))}
          <div className="detail-footer">
            <button className="danger" onClick={() => removeTask(active.id)}>
              <Trash2 size={16} />
              {tr(" 삭제 ")}
            </button>
            <button
              className="primary"
              onClick={() =>
                write(tasks.find((t) => t.id === active.id) ?? active)
              }
            >
              {tr(" 저장 ")}
            </button>
          </div>
        </aside>
      )}
      {error && (
        <div className="toast" onClick={() => setError("")}>
          {error} <X size={14} />
        </div>
      )}
    </div>
  );
}
export default App;
