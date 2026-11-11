import { useEffect, useMemo } from "react";
import { Archive, CalendarRange, CheckCircle2, Inbox, ListTodo, Repeat, Square, Sun, Timer } from "lucide-react";
import { api, events, fmtClock, isoDate } from "../lib/api";
import { useApp, type Section } from "../store";
import TaskRow from "../components/TaskRow";
import CycleView from "../components/CycleView";

const SECTIONS: { id: Section; label: string; icon: React.ReactNode; key: string }[] = [
  { id: "today", label: "오늘", icon: <Sun size={15} />, key: "1" },
  { id: "upcoming", label: "다음 7일", icon: <CalendarRange size={15} />, key: "2" },
  { id: "all", label: "전체", icon: <ListTodo size={15} />, key: "3" },
  { id: "backlog", label: "백로그", icon: <Archive size={15} />, key: "4" },
  { id: "done", label: "완료", icon: <CheckCircle2 size={15} />, key: "5" },
  { id: "cycle", label: "N일 주기", icon: <Repeat size={15} />, key: "6" },
];

const HINTS = "j/k 이동 · x 완료 · c 취소 · t 내일로 · f 포모도로 · ⌫ 삭제 · n 빠른 추가 · 1–6 화면";

export default function MainWindow() {
  const { section, tasks, selected, error, pomodoro, setSection, select, move, refresh, setPomodoro, setError } =
    useApp();

  useEffect(() => {
    void refresh();
    void api.pomodoroStatus().then(setPomodoro);
    const a = events.tasksChanged(() => void useApp.getState().refresh());
    const b = events.pomodoroTick(setPomodoro);
    return () => {
      void a.then((f) => f());
      void b.then((f) => f());
    };
  }, [refresh, setPomodoro]);

  const titles = useMemo(() => new Map(tasks.map((t) => [t.id, t.title])), [tasks]);
  const current = tasks[selected];

  // Keyboard-first control.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.ctrlKey || e.metaKey || e.altKey) return;
      const sec = SECTIONS.find((s) => s.key === e.key);
      if (sec) return setSection(sec.id);
      if (e.key === "n") return void api.toggleQuickAdd();
      if (section === "cycle") return;
      const run = (p: Promise<unknown>) => p.catch((err) => setError(String(err)));
      switch (e.key) {
        case "j":
        case "ArrowDown":
          e.preventDefault();
          return move(1);
        case "k":
        case "ArrowUp":
          e.preventDefault();
          return move(-1);
      }
      if (!current) return;
      switch (e.key) {
        case "x":
        case " ":
          e.preventDefault();
          return run(api.setStatus(current.id, current.status === "done" ? "todo" : "done"));
        case "c":
          return run(api.setStatus(current.id, "cancelled"));
        case "t": {
          const tmr = isoDate(new Date(Date.now() + 86_400_000));
          return run(api.reschedule(current.id, tmr, current.due_time));
        }
        case "f":
          return run(api.pomodoroStart(current.id, current.estimated_minutes && current.estimated_minutes < 25 ? current.estimated_minutes : 25));
        case "Backspace":
        case "Delete":
          return run(api.deleteTask(current.id));
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [section, current, move, setSection, setError]);

  const header = SECTIONS.find((s) => s.id === section)!;

  return (
    <div className="flex h-full">
      <aside className="flex w-52 shrink-0 flex-col border-r border-line bg-panel px-2 py-4">
        <div className="mb-4 px-3 text-sm font-semibold tracking-tight">BetterTick</div>
        <nav className="space-y-0.5">
          {SECTIONS.map((s) => (
            <button
              key={s.id}
              onClick={() => setSection(s.id)}
              className={`flex w-full items-center gap-2.5 rounded-md px-3 py-1.5 text-sm ${
                section === s.id ? "bg-raised text-fg" : "text-muted hover:bg-raised/60 hover:text-fg"
              }`}
            >
              {s.icon}
              <span className="flex-1 text-left">{s.label}</span>
              <kbd className="text-[10px] text-faint">{s.key}</kbd>
            </button>
          ))}
        </nav>
        <button
          onClick={() => void api.toggleQuickAdd()}
          className="mt-4 flex items-center gap-2 rounded-md border border-line px-3 py-1.5 text-sm text-muted hover:border-accent hover:text-fg"
        >
          <Inbox size={15} /> 빠른 추가
          <kbd className="ml-auto text-[10px] text-faint">Alt+Space</kbd>
        </button>
        <div className="flex-1" />
        {pomodoro?.running && (
          <div className="rounded-lg border border-line bg-bg p-3">
            <div className="flex items-center gap-2 text-[11px] text-muted">
              <Timer size={12} /> 집중 중
            </div>
            <div className="mt-1 font-mono text-xl tabular-nums">{fmtClock(pomodoro.remaining_secs)}</div>
            <div className="truncate text-[11px] text-faint">{pomodoro.task_title ?? "자유 집중"}</div>
            <button
              onClick={() => void api.pomodoroStop()}
              className="mt-2 flex items-center gap-1 text-[11px] text-muted hover:text-danger"
            >
              <Square size={11} /> 정지
            </button>
          </div>
        )}
      </aside>

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-line px-6 py-3">
          <h1 className="flex items-center gap-2 text-[15px] font-semibold">
            {header.icon}
            {header.label}
            {section !== "cycle" && <span className="text-xs font-normal text-faint">{tasks.length}</span>}
          </h1>
          {section === "backlog" && (
            <span className="text-[11px] text-faint">3일 넘게 밀리거나 3번 이상 미룬 할 일 · t 로 다시 꺼내기</span>
          )}
        </header>

        {error && (
          <div className="mx-6 mt-3 flex items-center justify-between rounded-md border border-danger/40 bg-danger/10 px-3 py-2 text-xs text-danger">
            {error}
            <button onClick={() => setError(null)}>닫기</button>
          </div>
        )}

        <div className="flex-1 overflow-y-auto px-4 py-3">
          {section === "cycle" ? (
            <div className="h-full px-2">
              <CycleView />
            </div>
          ) : tasks.length === 0 ? (
            <div className="mt-24 text-center text-sm text-faint">
              할 일이 없어요. <kbd className="text-muted">n</kbd> 또는 <kbd className="text-muted">Alt+Space</kbd> 로
              추가해 보세요.
            </div>
          ) : (
            <div className="space-y-0.5">
              {tasks.map((t, i) => (
                <TaskRow
                  key={t.id}
                  task={t}
                  selected={i === selected}
                  onSelect={() => select(i)}
                  titleOf={(id) => titles.get(id)}
                  onToggle={() =>
                    void api.setStatus(t.id, t.status === "done" ? "todo" : "done").catch((e) => setError(String(e)))
                  }
                />
              ))}
            </div>
          )}
        </div>

        <footer className="border-t border-line px-6 py-2 text-[11px] text-faint">{HINTS}</footer>
      </main>
    </div>
  );
}
