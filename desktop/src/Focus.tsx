import { tr } from "./i18n";
import { useEffect, useRef, useState } from "react";
import type { FocusSession, Task } from "./domain";
import type { Preferences } from "./Settings";
type TimerState = {
  mode: "focus" | "short" | "long" | "stopwatch";
  remaining: number;
  running: boolean;
  deadline: number;
  startedAt: number;
  segmentStart: number;
  elapsed: number;
  taskId: string;
  round: number;
};

export function Focus({
  storageKey,
  prefs,
  tasks,
  sessions,
  onSession,
  onNotify,
}: {
  storageKey: string;
  prefs: Preferences;
  tasks: Task[];
  sessions: FocusSession[];
  onSession: (s: FocusSession) => void;
  onNotify: (s: string) => void;
}) {
  const key = storageKey;
  const minutes = (mode: TimerState["mode"]) =>
    mode === "short"
      ? prefs.shortBreakMinutes
      : mode === "long"
        ? prefs.longBreakMinutes
        : prefs.focusMinutes;
  const fresh = (mode: TimerState["mode"] = "focus"): TimerState => ({
    mode,
    remaining: mode === "stopwatch" ? 0 : minutes(mode) * 60,
    running: false,
    deadline: 0,
    startedAt: 0,
    segmentStart: 0,
    elapsed: 0,
    taskId: "",
    round: 0,
  });
  const [timer, setTimer] = useState<TimerState>(() => {
    try {
      const old = JSON.parse(localStorage.getItem(key) || "null");
      return old &&
        ["focus", "short", "long", "stopwatch"].includes(old.mode) &&
        [
          "remaining",
          "deadline",
          "startedAt",
          "segmentStart",
          "elapsed",
          "round",
        ].every((k) => Number.isFinite(old[k]) && old[k] >= 0) &&
        typeof old.running === "boolean" &&
        typeof old.taskId === "string"
        ? old
        : fresh();
    } catch {
      return fresh();
    }
  });
  const saveRef = useRef(onSession);
  saveRef.current = onSession;
  const notifyRef = useRef(onNotify);
  notifyRef.current = onNotify;
  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(timer));
  }, [timer]);
  useEffect(() => {
    if (!timer.running) return;
    const tick = () => {
      const now = Date.now();
      const seconds =
        timer.mode === "stopwatch"
          ? timer.elapsed + Math.floor((now - timer.deadline) / 1000)
          : Math.max(0, Math.ceil((timer.deadline - now) / 1000));
      if (timer.mode !== "stopwatch" && seconds === 0) {
        if (timer.mode === "focus") {
          const id = "focus-" + timer.startedAt;
          saveRef.current({
            id,
            taskId: timer.taskId,
            startedAt: new Date(timer.startedAt).toISOString(),
            endedAt: new Date(timer.deadline).toISOString(),
            seconds:
              timer.elapsed +
              Math.round((timer.deadline - timer.segmentStart) / 1000),
          });
        }
        if (prefs.focusSound) {
          try {
            const audio = new AudioContext();
            const osc = audio.createOscillator(),
              gain = audio.createGain();
            osc.connect(gain);
            gain.connect(audio.destination);
            gain.gain.value = 0.08;
            osc.frequency.value = 660;
            osc.start();
            osc.stop(audio.currentTime + 0.25);
            osc.onended = () => {
              void audio.close();
            };
          } catch {}
        }
        notifyRef.current(
          timer.mode === "focus"
            ? tr("이번 집중을 완료했어요. 잠시 쉬어가세요.")
            : tr("휴식이 끝났어요."),
        );
        setTimer((t) => ({
          ...t,
          remaining: 0,
          running: false,
          startedAt: 0,
          segmentStart: 0,
          elapsed: 0,
          round: t.mode === "focus" ? t.round + 1 : t.round,
        }));
      } else setTimer((t) => ({ ...t, remaining: seconds }));
    };
    const id = setInterval(tick, 250);
    tick();
    return () => clearInterval(id);
  }, [timer.running, timer.deadline]);
  useEffect(() => {
    setTimer((t) =>
      !t.running && !t.startedAt && t.mode !== "stopwatch"
        ? { ...t, remaining: minutes(t.mode) * 60 }
        : t,
    );
  }, [prefs.focusMinutes, prefs.shortBreakMinutes, prefs.longBreakMinutes]);
  function toggle() {
    const now = Date.now();
    if (timer.running) {
      const elapsed =
        timer.mode === "stopwatch"
          ? timer.remaining
          : timer.elapsed +
            Math.max(0, Math.floor((now - timer.segmentStart) / 1000));
      setTimer({ ...timer, running: false, elapsed });
    } else {
      const remaining =
        timer.remaining ||
        (timer.mode === "stopwatch" ? 0 : minutes(timer.mode) * 60);
      setTimer({
        ...timer,
        remaining,
        running: true,
        startedAt: timer.startedAt || now,
        segmentStart: now,
        deadline: timer.mode === "stopwatch" ? now : now + remaining * 1000,
      });
    }
  }
  function finish() {
    const seconds =
      timer.mode === "stopwatch"
        ? timer.remaining
        : timer.elapsed +
          (timer.running
            ? Math.max(0, Math.floor((Date.now() - timer.segmentStart) / 1000))
            : 0);
    if (seconds > 0 && timer.startedAt)
      onSession({
        id: "focus-" + timer.startedAt,
        taskId: timer.taskId,
        startedAt: new Date(timer.startedAt).toISOString(),
        endedAt: new Date().toISOString(),
        seconds,
      });
    setTimer({
      ...fresh(timer.mode),
      taskId: timer.taskId,
      round: timer.round,
    });
  }
  return (
    <section className="focus">
      <div className="view-controls">
        {(
          [
            ["focus", tr("뽀모도로")],
            ["short", tr("짧은 휴식")],
            ["long", tr("긴 휴식")],
            ["stopwatch", tr("스톱워치")],
          ] as const
        ).map(([mode, label]) => (
          <button
            key={mode}
            className={timer.mode === mode ? "active" : ""}
            onClick={() => {
              if (
                timer.running &&
                !confirm(tr("진행 중인 타이머를 초기화할까요?"))
              )
                return;
              setTimer({
                ...fresh(mode),
                taskId: timer.taskId,
                round: timer.round,
              });
            }}
          >
            {label}
          </button>
        ))}
      </div>
      <h1>{tr("집중 타이머")}</h1>
      <select
        aria-label={tr("집중할 할 일")}
        value={timer.taskId}
        disabled={timer.running}
        onChange={(e) => setTimer({ ...timer, taskId: e.target.value })}
      >
        <option value="">{tr("할 일 없이 집중")}</option>
        {tasks
          .filter((t) => !t.isCompleted && !t.deletedAt)
          .map((t) => (
            <option key={t.id} value={t.id}>
              {t.title}
            </option>
          ))}
      </select>
      <div className="timer">
        {String(Math.floor(timer.remaining / 60)).padStart(2, "0")}:
        {String(timer.remaining % 60).padStart(2, "0")}
      </div>
      <div className="actions">
        <button className="primary" onClick={toggle}>
          {timer.running ? tr("일시정지") : tr("시작")}
        </button>
        <button
          onClick={() =>
            setTimer({
              ...fresh(timer.mode),
              taskId: timer.taskId,
              round: timer.round,
            })
          }
        >
          {tr(" 초기화 ")}
        </button>
        <button onClick={finish}>{tr("기록하고 종료")}</button>
      </div>
      <p>
        {timer.round}
        {tr("회 완료 · 오늘 집중")}{" "}
        {Math.floor(
          sessions
            .filter(
              (s) =>
                s.endedAt.slice(0, 10) ===
                new Date().toISOString().slice(0, 10),
            )
            .reduce((n, s) => n + s.seconds, 0) / 60,
        )}
        {tr(" 분 ")}
      </p>
      <button
        onClick={() =>
          setTimer({
            ...fresh(
              timer.round > 0 && timer.round % 4 === 0 ? "long" : "short",
            ),
            taskId: timer.taskId,
            round: timer.round,
          })
        }
      >
        {tr(" 휴식 준비 ")}
      </button>
      <h3>{tr("최근 집중 기록")}</h3>
      <div className="session-list">
        {sessions
          .slice()
          .sort((a, b) => b.endedAt.localeCompare(a.endedAt))
          .slice(0, 8)
          .map((s) => (
            <div key={s.id}>
              <span>
                {tasks.find((t) => t.id === s.taskId)?.title ?? tr("자유 집중")}
              </span>
              <span>
                {Math.max(1, Math.round(s.seconds / 60))}
                {tr("분 ·")} {new Date(s.endedAt).toLocaleString()}
              </span>
            </div>
          ))}
      </div>
    </section>
  );
}
