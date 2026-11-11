import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { dateKey, taskDate, type Task } from "./domain";
import type { Preferences } from "./Settings";
export function Calendar({
  tasks,
  prefs,
  onSave,
  onSelect,
  onAdd,
}: {
  tasks: Task[];
  prefs: Preferences;
  onSave: (t: Task) => void;
  onSelect: (id: string) => void;
  onAdd: (date: string) => void;
}) {
  const [month, setMonth] = useState(new Date()),
    [mode, setMode] = useState("month");
  const active = tasks.filter(
    (t) => !t.deletedAt && (!t.isCompleted || prefs.showCompleted),
  );
  const first = new Date(month.getFullYear(), month.getMonth(), 1),
    offset = (first.getDay() - prefs.weekStartsOn + 7) % 7;
  const dayStart = new Date(month);
  dayStart.setHours(0, 0, 0, 0);
  const weekStart = new Date(dayStart);
  weekStart.setDate(
    weekStart.getDate() - ((weekStart.getDay() - prefs.weekStartsOn + 7) % 7),
  );
  const dates =
    mode === "month"
      ? Array.from(
          {
            length:
              Math.ceil(
                (offset +
                  new Date(
                    month.getFullYear(),
                    month.getMonth() + 1,
                    0,
                  ).getDate()) /
                  7,
              ) * 7,
          },
          (_, i) =>
            new Date(month.getFullYear(), month.getMonth(), i - offset + 1),
        )
      : Array.from({ length: mode === "day" ? 1 : 7 }, (_, i) => {
          const d = new Date(mode === "day" ? dayStart : weekStart);
          d.setDate(d.getDate() + i);
          return d;
        });
  const drop = (e: React.DragEvent, date: Date, time?: string) => {
    e.preventDefault();
    const t = tasks.find((t) => t.id === e.dataTransfer.getData("text/plain"));
    if (t)
      onSave({
        ...t,
        dueDate: date.toISOString(),
        ...(time ? { dueTime: time } : {}),
      });
  };
  const card = (t: Task) => (
    <button
      className={"calendar-task " + (t.isCompleted ? "done" : "")}
      draggable
      key={t.id}
      onDragStart={(e) => {
        e.stopPropagation();
        e.dataTransfer.setData("text/plain", t.id);
      }}
      onClick={(e) => {
        e.stopPropagation();
        onSelect(t.id);
      }}
    >
      {t.dueTime && <b>{t.dueTime} </b>}
      {t.title}
    </button>
  );
  function move(dir: number) {
    if (mode === "month")
      setMonth(new Date(month.getFullYear(), month.getMonth() + dir, 1));
    else {
      const d = new Date(month);
      d.setDate(d.getDate() + dir * (mode === "day" ? 1 : 7));
      setMonth(d);
    }
  }
  return (
    <section className="content calendar-content">
      <div className="heading">
        <div>
          <h1>캘린더</h1>
          <p>할 일을 날짜 또는 시간 칸으로 끌어 놓으세요</p>
        </div>
        <div className="actions">
          <button aria-label="이전 기간" onClick={() => move(-1)}>
            <ChevronLeft size={18} />
          </button>
          <strong>
            {month.getFullYear()}년 {month.getMonth() + 1}월
          </strong>
          <button aria-label="다음 기간" onClick={() => move(1)}>
            <ChevronRight size={18} />
          </button>
        </div>
      </div>
      <div className="view-controls">
        <button onClick={() => setMonth(new Date())}>오늘로</button>
        {[
          ["month", "월"],
          ["week", "주"],
          ["day", "일"],
          ["agenda", "일정"],
        ].map(([id, label]) => (
          <button
            className={mode === id ? "active" : ""}
            key={id}
            onClick={() => setMode(id)}
          >
            {label}
          </button>
        ))}
      </div>
      <div className="unscheduled">
        <span>날짜 미지정</span>
        {active.filter((t) => !taskDate(t)).map(card)}
      </div>
      {mode === "agenda" ? (
        <div className="agenda">
          {active
            .filter((t) => taskDate(t))
            .sort(
              (a, b) =>
                taskDate(a)!.getTime() - taskDate(b)!.getTime() ||
                (a.dueTime ?? "").localeCompare(b.dueTime ?? ""),
            )
            .map((t) => (
              <div key={t.id}>
                <time>{dateKey(taskDate(t)!)}</time>
                {card(t)}
              </div>
            ))}
        </div>
      ) : mode === "month" ? (
        <div className="calendar">
          {Array.from(
            { length: 7 },
            (_, i) =>
              ["일", "월", "화", "수", "목", "금", "토"][
                (i + prefs.weekStartsOn) % 7
              ],
          ).map((d) => (
            <div className="weekday" key={d}>
              {d}
            </div>
          ))}
          {dates.map((date) => (
            <div
              key={dateKey(date)}
              data-date={dateKey(date)}
              className={
                "day " + (date.getMonth() !== month.getMonth() ? "muted" : "")
              }
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => drop(e, date)}
              onDoubleClick={() => onAdd(dateKey(date))}
            >
              <button
                aria-label={dateKey(date) + " 할 일 추가"}
                className={
                  dateKey(date) === dateKey(new Date()) ? "today-pill" : ""
                }
                onClick={() => onAdd(dateKey(date))}
              >
                {date.getDate()}
              </button>
              {active
                .filter(
                  (t) => taskDate(t) && dateKey(taskDate(t)!) === dateKey(date),
                )
                .map(card)}
            </div>
          ))}
        </div>
      ) : (
        <div
          className="time-calendar"
          style={{
            gridTemplateColumns: `55px repeat(${dates.length},minmax(100px,1fr))`,
          }}
        >
          <div />
          {dates.map((d) => (
            <div className="weekday" key={dateKey(d)}>
              {dateKey(d).slice(5)}
            </div>
          ))}
          {Array.from({ length: 24 }, (_, hour) => (
            <div className="time-row" key={hour}>
              <time>{String(hour).padStart(2, "0")}:00</time>
              {dates.map((d) => (
                <div
                  className="time-cell"
                  key={dateKey(d)}
                  data-slot={
                    dateKey(d) + "T" + String(hour).padStart(2, "0") + ":00"
                  }
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) =>
                    drop(e, d, String(hour).padStart(2, "0") + ":00")
                  }
                  onDoubleClick={() => onAdd(dateKey(d))}
                >
                  {active
                    .filter(
                      (t) =>
                        taskDate(t) &&
                        dateKey(taskDate(t)!) === dateKey(d) &&
                        Number((t.dueTime ?? "09:00").split(":")[0]) === hour,
                    )
                    .map(card)}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
