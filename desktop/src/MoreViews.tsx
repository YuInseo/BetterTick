import { useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { dateKey, taskDate, type Task } from "./domain";
export type Countdown = {
  id: string;
  name: string;
  date: string;
  color: string;
  yearly: boolean;
};
export function Countdowns({
  items,
  onSave,
  onDelete,
}: {
  items: Countdown[];
  onSave: (c: Countdown) => void;
  onDelete: (id: string) => void;
}) {
  const [editing, setEditing] = useState<Countdown | null>(null);
  const remaining = (c: Countdown) => {
    const now = new Date(),
      today = new Date(dateKey(now) + "T12:00:00"),
      target = new Date(c.date + "T12:00:00");
    if (c.yearly) {
      const month = target.getMonth(),
        day = target.getDate();
      target.setDate(1);
      target.setFullYear(now.getFullYear());
      target.setMonth(month);
      target.setDate(
        Math.min(day, new Date(target.getFullYear(), month + 1, 0).getDate()),
      );
      if (target < today) {
        target.setDate(1);
        target.setFullYear(now.getFullYear() + 1);
        target.setMonth(month);
        target.setDate(
          Math.min(day, new Date(target.getFullYear(), month + 1, 0).getDate()),
        );
      }
    }
    return Math.round(
      (Date.UTC(target.getFullYear(), target.getMonth(), target.getDate()) -
        Date.UTC(today.getFullYear(), today.getMonth(), today.getDate())) /
        86400000,
    );
  };
  return (
    <section className="content">
      <div className="heading">
        <div>
          <h1>디데이</h1>
          <p>기념일과 중요한 날을 기억하세요</p>
        </div>
        <button
          className="primary"
          onClick={() =>
            setEditing({
              id: crypto.randomUUID(),
              name: "",
              date: dateKey(new Date()),
              color: "#4776ef",
              yearly: false,
            })
          }
        >
          디데이 추가
        </button>
      </div>
      {editing && (
        <form
          className="habit-editor"
          onSubmit={(e) => {
            e.preventDefault();
            if (editing.name.trim() && editing.date) {
              onSave({ ...editing, name: editing.name.trim() });
              setEditing(null);
            }
          }}
        >
          <label>
            이름
            <input
              required
              aria-label="디데이 이름"
              value={editing.name}
              onChange={(e) => setEditing({ ...editing, name: e.target.value })}
            />
          </label>
          <label>
            날짜
            <input
              required
              type="date"
              aria-label="디데이 날짜"
              value={editing.date}
              onChange={(e) => setEditing({ ...editing, date: e.target.value })}
            />
          </label>
          <input
            aria-label="디데이 색상"
            type="color"
            value={editing.color}
            onChange={(e) => setEditing({ ...editing, color: e.target.value })}
          />
          <label>
            <input
              type="checkbox"
              checked={editing.yearly}
              onChange={(e) =>
                setEditing({ ...editing, yearly: e.target.checked })
              }
            />
            매년 반복
          </label>
          <button className="primary">디데이 저장</button>
          <button type="button" onClick={() => setEditing(null)}>
            취소
          </button>
        </form>
      )}
      <div className="countdown-grid">
        {items
          .slice()
          .sort((a, b) => remaining(a) - remaining(b))
          .map((c) => {
            const n = remaining(c);
            return (
              <article key={c.id} style={{ borderTopColor: c.color }}>
                <button
                  className="countdown-edit"
                  onClick={() => setEditing(c)}
                >
                  <span>{c.name}</span>
                  <strong style={{ color: c.color }}>
                    {n === 0 ? "D-Day" : n > 0 ? "D-" + n : "D+" + Math.abs(n)}
                  </strong>
                  <time>
                    {c.date}
                    {c.yearly ? " · 매년" : ""}
                  </time>
                </button>
                <button
                  className="danger"
                  onClick={() => {
                    if (confirm("디데이를 삭제할까요?")) onDelete(c.id);
                  }}
                >
                  삭제
                </button>
              </article>
            );
          })}
      </div>
    </section>
  );
}
export function Markdown({ text }: { text: string }) {
  return (
    <div className="markdown">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          a: ({ children, href }) => (
            <a href={href} target="_blank" rel="noreferrer">
              {children}
            </a>
          ),
        }}
      >
        {text}
      </ReactMarkdown>
    </div>
  );
}
export function Timeline({
  tasks,
  onSelect,
  onSave,
}: {
  tasks: Task[];
  onSelect: (id: string) => void;
  onSave: (t: Task) => void;
}) {
  const [week, setWeek] = useState(0);
  const dates = Array.from({ length: 14 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i + week * 7);
    return dateKey(d);
  });
  return (
    <section className="content">
      <div className="heading">
        <div>
          <h1>타임라인</h1>
          <p>기간을 지정하고 일정을 끌어서 이동하세요</p>
        </div>
        <div className="view-controls">
          <button onClick={() => setWeek((n) => n - 1)}>이전</button>
          <button onClick={() => setWeek(0)}>오늘</button>
          <button onClick={() => setWeek((n) => n + 1)}>다음</button>
        </div>
      </div>
      <div
        className="timeline"
        style={{ gridTemplateColumns: "200px repeat(14,minmax(80px,1fr))" }}
      >
        <div />
        {dates.map((d) => (
          <time key={d}>{d.slice(5)}</time>
        ))}
        {tasks
          .filter((t) => !t.deletedAt && !t.isCompleted)
          .map((t) => {
            const end = taskDate(t) ? dateKey(taskDate(t)!) : "",
              start = t.startDate || end;
            return (
              <div className="timeline-row" key={t.id}>
                <button onClick={() => onSelect(t.id)}>{t.title}</button>
                {dates.map((date) => (
                  <div
                    key={date}
                    className={
                      date >= start && date <= end ? "timeline-bar" : ""
                    }
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => {
                      e.preventDefault();
                      const target = tasks.find(
                        (x) => x.id === e.dataTransfer.getData("text/plain"),
                      );
                      if (!target) return;
                      const length =
                        target.startDate && taskDate(target)
                          ? Math.max(
                              0,
                              Math.round(
                                (taskDate(target)!.getTime() -
                                  new Date(
                                    target.startDate + "T12:00:00",
                                  ).getTime()) /
                                  86400000,
                              ),
                            )
                          : 0;
                      const finish = new Date(date + "T12:00:00");
                      finish.setDate(finish.getDate() + length);
                      onSave({
                        ...target,
                        startDate: date,
                        dueDate: finish.toISOString(),
                      });
                    }}
                  >
                    {date === start && (
                      <button
                        draggable
                        onDragStart={(e) =>
                          e.dataTransfer.setData("text/plain", t.id)
                        }
                        onClick={() => onSelect(t.id)}
                      >
                        {t.title}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            );
          })}
      </div>
    </section>
  );
}
