import { tr } from "./i18n";
import { useState } from "react";
import {
  dateKey,
  habitStreak,
  taskDate,
  type Habit,
  type HabitLog,
  type Task,
  type FocusSession,
} from "./domain";
export function Habits({
  habits,
  logs,
  onSave,
  onLog,
  onDelete,
}: {
  habits: Habit[];
  logs: HabitLog[];
  onSave: (h: Habit) => void;
  onLog: (l: HabitLog) => void;
  onDelete: (id: string) => void;
}) {
  const [offset, setOffset] = useState(0),
    [archive, setArchive] = useState(false),
    [editing, setEditing] = useState<Habit | null>(null);
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - 6 + i + offset * 7);
    return dateKey(d);
  });
  const today = dateKey(new Date());
  const add = () =>
    setEditing({
      id: crypto.randomUUID(),
      name: "",
      color: "#4776ef",
      target: 1,
      unit: tr("회"),
      targetDays: [0, 1, 2, 3, 4, 5, 6],
      isArchived: false,
    });
  return (
    <section className="content">
      <div className="heading">
        <div>
          <h1>{tr("습관")}</h1>
          <p>{tr("작은 실천을 매일 쌓아보세요")}</p>
        </div>
        <button className="primary" onClick={add}>
          {tr(" + 습관 추가 ")}
        </button>
      </div>
      <div className="view-controls">
        <button onClick={() => setOffset((o) => o - 1)}>{tr("이전 주")}</button>
        <button onClick={() => setOffset(0)}>{tr("오늘")}</button>
        <button disabled={offset >= 0} onClick={() => setOffset((o) => o + 1)}>
          {tr(" 다음 주 ")}
        </button>
        <label>
          <input
            type="checkbox"
            checked={archive}
            onChange={(e) => setArchive(e.target.checked)}
          />{" "}
          {tr(" 보관한 습관 ")}
        </label>
      </div>
      {editing && (
        <form
          className="habit-editor"
          onSubmit={(e) => {
            e.preventDefault();
            if (!editing.name.trim() || !editing.targetDays.length) return;
            onSave({ ...editing, name: editing.name.trim() });
            setEditing(null);
          }}
        >
          <h3>{tr("습관 설정")}</h3>
          <label>
            {tr(" 이름 ")}
            <input
              required
              aria-label={tr("습관 이름")}
              value={editing.name}
              onChange={(e) => setEditing({ ...editing, name: e.target.value })}
            />
          </label>
          <label>
            {tr(" 목표 ")}
            <input
              type="number"
              aria-label={tr("습관 목표")}
              min="1"
              max="9999"
              value={editing.target}
              onChange={(e) =>
                setEditing({
                  ...editing,
                  target: Math.max(1, Number(e.target.value)),
                })
              }
            />
          </label>
          <label>
            {tr(" 단위 ")}
            <input
              aria-label={tr("습관 단위")}
              value={editing.unit}
              onChange={(e) => setEditing({ ...editing, unit: e.target.value })}
            />
          </label>
          <label>
            {tr(" 색상 ")}
            <input
              type="color"
              value={editing.color}
              onChange={(e) =>
                setEditing({ ...editing, color: e.target.value })
              }
            />
          </label>
          <div className="weekdays">
            {[
              tr("일"),
              tr("월"),
              tr("화"),
              tr("수"),
              tr("목"),
              tr("금"),
              tr("토"),
            ].map((d, i) => (
              <label key={d}>
                <input
                  type="checkbox"
                  checked={editing.targetDays.includes(i)}
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      targetDays: e.target.checked
                        ? [...editing.targetDays, i]
                        : editing.targetDays.filter((x) => x !== i),
                    })
                  }
                />
                {d}
              </label>
            ))}
          </div>
          <button className="primary" disabled={!editing.targetDays.length}>
            {tr(" 습관 저장 ")}
          </button>
          <button type="button" onClick={() => setEditing(null)}>
            {tr(" 취소 ")}
          </button>
        </form>
      )}
      <div className="habit-table">
        <div className="habit-head">
          <span>{tr("습관 / 연속 달성")}</span>
          {days.map((d) => (
            <span key={d}>{d.slice(5)}</span>
          ))}
        </div>
        {habits
          .filter((h) => h.isArchived === archive)
          .map((h) => (
            <div className="habit-row" key={h.id}>
              <div>
                <button
                  className="habit-name"
                  onClick={() => setEditing(h)}
                  style={{ color: h.color }}
                >
                  {h.name}
                </button>
                <small>
                  {habitStreak(h, logs)}
                  {tr("일 연속 · 목표 ")}
                  {h.target}
                  {h.unit}
                </small>
                <button
                  className="habit-remove"
                  onClick={() => onSave({ ...h, isArchived: !h.isArchived })}
                >
                  {h.isArchived ? tr("복원") : tr("보관")}
                </button>
                <button
                  className="habit-remove"
                  onClick={() => {
                    if (confirm(tr("습관과 기록을 삭제할까요?")))
                      onDelete(h.id);
                  }}
                >
                  {tr(" 삭제 ")}
                </button>
              </div>
              {days.map((d) => {
                const value =
                  logs.find((l) => l.habitId === h.id && l.date === d)?.value ??
                  0;
                const scheduled = h.targetDays.includes(
                  new Date(d + "T12:00:00").getDay(),
                );
                return (
                  <button
                    key={d}
                    aria-label={h.name + " " + d}
                    disabled={d > today || !scheduled || h.isArchived}
                    className={
                      "habit-check " + (value >= h.target ? "is-done" : "")
                    }
                    onContextMenu={(e) => {
                      e.preventDefault();
                      onLog({
                        id: h.id + "_" + d,
                        habitId: h.id,
                        date: d,
                        value: 0,
                      });
                    }}
                    onClick={() =>
                      onLog({
                        id: h.id + "_" + d,
                        habitId: h.id,
                        date: d,
                        value: value >= h.target ? 0 : value + 1,
                      })
                    }
                  >
                    {!scheduled
                      ? "—"
                      : value >= h.target
                        ? "✓"
                        : `${value}/${h.target}`}
                  </button>
                );
              })}
            </div>
          ))}
      </div>
      {!habits.some((h) => h.isArchived === archive) && (
        <div className="empty">
          <h3>{tr("첫 습관을 추가해 보세요")}</h3>
        </div>
      )}
    </section>
  );
}
export function Matrix({
  tasks,
  onSelect,
  onMove,
}: {
  tasks: Task[];
  onSelect: (id: string) => void;
  onMove: (t: Task) => void;
}) {
  const today = dateKey(new Date());
  const qs = [
    { title: tr("긴급하고 중요"), important: true, urgent: true },
    { title: tr("중요하지만 긴급하지 않음"), important: true, urgent: false },
    { title: tr("긴급하지만 덜 중요"), important: false, urgent: true },
    { title: tr("긴급하지 않고 덜 중요"), important: false, urgent: false },
  ];
  return (
    <section className="content">
      <div className="heading">
        <div>
          <h1>{tr("아이젠하워 매트릭스")}</h1>
          <p>{tr("카드를 끌어서 중요도와 일정을 변경하세요")}</p>
        </div>
      </div>
      <div className="matrix">
        {qs.map((q, i) => (
          <div
            className={"matrix-cell matrix-" + i}
            key={q.title}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              const t = tasks.find(
                (t) => t.id === e.dataTransfer.getData("text/plain"),
              );
              if (!t) return;
              const date = new Date();
              if (!q.urgent) date.setDate(date.getDate() + 1);
              onMove({
                ...t,
                priority: q.important ? 3 : 1,
                dueDate: date.toISOString(),
              });
            }}
          >
            <h3>{q.title}</h3>
            {tasks
              .filter((t) => {
                const d = taskDate(t);
                return (
                  !t.isCompleted &&
                  !t.deletedAt &&
                  (t.priority === 3) === q.important &&
                  (!!d && dateKey(d) <= today) === q.urgent
                );
              })
              .map((t) => (
                <button
                  draggable
                  onDragStart={(e) =>
                    e.dataTransfer.setData("text/plain", t.id)
                  }
                  key={t.id}
                  onClick={() => onSelect(t.id)}
                >
                  {t.title}
                </button>
              ))}
          </div>
        ))}
      </div>
    </section>
  );
}
export function Statistics({
  tasks,
  habits,
  logs,
  sessions,
}: {
  tasks: Task[];
  habits: Habit[];
  logs: HabitLog[];
  sessions: FocusSession[];
}) {
  const complete = tasks.filter((t) => t.isCompleted && !t.deletedAt),
    total = tasks.filter((t) => !t.deletedAt).length;
  return (
    <section className="content">
      <div className="heading">
        <h1>{tr("통계")}</h1>
      </div>
      <div className="stat-cards">
        <article>
          <span>{tr("완료한 할 일")}</span>
          <strong>{complete.length}</strong>
          <small>
            {tr(" 전체 ")}
            {total}
            {tr("개 중")}{" "}
            {total ? Math.round((complete.length / total) * 100) : 0}%
          </small>
        </article>
        <article>
          <span>{tr("집중 시간")}</span>
          <strong>
            {Math.floor(sessions.reduce((n, s) => n + s.seconds, 0) / 60)}
            {tr("분 ")}
          </strong>
          <small>
            {sessions.length}
            {tr("번의 집중")}
          </small>
        </article>
        <article>
          <span>{tr("습관 달성")}</span>
          <strong>
            {
              logs.filter(
                (l) =>
                  l.value >=
                  (habits.find((h) => h.id === l.habitId)?.target ?? Infinity),
              ).length
            }
            {tr(" 회 ")}
          </strong>
          <small>{tr("실천 기록")}</small>
        </article>
      </div>
      <h2>{tr("최근 7일")}</h2>
      <div className="stats-days">
        {Array.from({ length: 7 }, (_, i) => {
          const d = new Date();
          d.setDate(d.getDate() - 6 + i);
          const key = dateKey(d),
            count = complete.filter((t) => {
              const date =
                typeof t.completedAt === "string"
                  ? new Date(t.completedAt)
                  : null;
              return date && dateKey(date) === key;
            }).length;
          return (
            <div key={key}>
              <span>{key.slice(5)}</span>
              <progress max={Math.max(10, count)} value={count} />
              <strong>
                {count}
                {tr("개 완료")}
              </strong>
            </div>
          );
        })}
      </div>
    </section>
  );
}
