import { Brain, Dumbbell, Lock, Repeat, Timer, Coffee } from "lucide-react";
import { ENERGY_LABEL, fmtMinutes, friendlyDate, PRIORITY_COLOR, type Task } from "../lib/api";

const ENERGY_ICON = {
  high_focus: <Brain size={12} />,
  low_routine: <Coffee size={12} />,
  physical: <Dumbbell size={12} />,
} as const;

export default function TaskRow({
  task,
  selected,
  onSelect,
  onToggle,
  titleOf,
}: {
  task: Task;
  selected: boolean;
  onSelect: () => void;
  onToggle: () => void;
  /** Resolves blocker ids to titles for the "waiting on" hint. */
  titleOf: (id: string) => string | undefined;
}) {
  const done = task.status === "done" || task.status === "cancelled";
  const today = new Date().toISOString().slice(0, 10);
  const overdue = !done && task.due_date !== null && task.due_date < today;
  const blockers = task.blocked_by.map(titleOf).filter(Boolean);

  return (
    <div
      onClick={onSelect}
      className={`group flex items-center gap-3 rounded-lg px-3 py-2 text-sm ${
        selected ? "bg-raised ring-1 ring-line" : "hover:bg-raised/60"
      } ${task.is_blocked ? "opacity-50" : ""}`}
    >
      <button
        onClick={(e) => {
          e.stopPropagation();
          onToggle();
        }}
        disabled={task.is_blocked && !done}
        className={`flex size-4 shrink-0 items-center justify-center rounded border ${
          done ? "border-accent bg-accent text-white" : "border-faint hover:border-accent"
        } ${PRIORITY_COLOR[task.priority]}`}
        title={task.is_blocked ? "선행 작업이 끝나야 해요" : "완료 (x)"}
      >
        {done && <span className="text-[10px] leading-none">✓</span>}
        {task.is_blocked && !done && <Lock size={9} />}
      </button>

      <div className="min-w-0 flex-1">
        <div className={`truncate ${done ? "text-faint line-through" : "text-fg"}`}>{task.title}</div>
        {task.is_blocked && blockers.length > 0 && (
          <div className="truncate text-[11px] text-faint">대기 중: {blockers.join(", ")}</div>
        )}
      </div>

      <div className="flex shrink-0 items-center gap-2 text-[11px] text-muted">
        {task.tags.map((t) => (
          <span key={t} className="rounded bg-raised px-1.5 py-0.5 text-faint">
            #{t}
          </span>
        ))}
        {task.energy_level && (
          <span className="flex items-center gap-1" title={ENERGY_LABEL[task.energy_level]}>
            {ENERGY_ICON[task.energy_level]}
          </span>
        )}
        {task.estimated_minutes && (
          <span className="flex items-center gap-1">
            <Timer size={12} />
            {fmtMinutes(task.estimated_minutes)}
          </span>
        )}
        {task.cycle_label && (
          <span className="flex items-center gap-1 text-accent">
            <Repeat size={12} />
            {task.cycle_label}
          </span>
        )}
        {task.postpone_count > 0 && <span className="text-warn">↻{task.postpone_count}</span>}
        {(task.due_date || task.due_time) && (
          <span className={`w-20 text-right ${overdue ? "text-danger" : ""}`}>
            {friendlyDate(task.due_date)} {task.due_time ?? ""}
          </span>
        )}
      </div>
    </div>
  );
}
