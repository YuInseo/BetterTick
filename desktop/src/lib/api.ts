import { invoke } from "@tauri-apps/api/core";
import { listen, type UnlistenFn } from "@tauri-apps/api/event";

export type EnergyLevel = "high_focus" | "low_routine" | "physical";
export type TaskStatus = "todo" | "done" | "cancelled" | "backlog";
export type TaskView = "today" | "upcoming" | "all" | "backlog" | "done";
export type FocusCategory = "focus" | "neutral" | "distraction";

export interface Task {
  id: string;
  title: string;
  notes: string;
  status: TaskStatus;
  priority: number;
  due_date: string | null;
  due_time: string | null;
  energy_level: EnergyLevel | null;
  estimated_minutes: number | null;
  cycle_pattern_id: number | null;
  cycle_label: string | null;
  postpone_count: number;
  tags: string[];
  blocked_by: string[];
  is_blocked: boolean;
  created_at: string;
  completed_at: string | null;
}

export interface NewTask {
  title: string;
  notes?: string;
  priority?: number;
  due_date?: string | null;
  due_time?: string | null;
  energy_level?: EnergyLevel | null;
  estimated_minutes?: number | null;
  cycle_pattern_id?: number | null;
  cycle_label?: string | null;
  tags?: string[];
  blocked_by?: string[];
}

export interface ParsedQuickAdd {
  title: string;
  due_date: string | null;
  due_time: string | null;
  tags: string[];
  priority: number;
  estimated_minutes: number | null;
  energy_level: EnergyLevel | null;
}

export interface CycleSlot {
  label: string;
  color: string | null;
}

export interface CyclePattern {
  id: number;
  name: string;
  anchor_date: string;
  slots: CycleSlot[];
  is_active: boolean;
}

export interface CycleDay {
  date: string;
  label: string;
  color: string | null;
  overridden: boolean;
}

export interface PomodoroTick {
  running: boolean;
  session_id: number | null;
  task_id: string | null;
  task_title: string | null;
  total_secs: number;
  remaining_secs: number;
  current_app: string | null;
  current_category: FocusCategory | null;
}

export const api = {
  listTasks: (view: TaskView) => invoke<Task[]>("list_tasks", { view }),
  createTask: (task: NewTask) => invoke<Task>("create_task", { task }),
  parseQuickAdd: (text: string) => invoke<ParsedQuickAdd>("parse_quick_add", { text }),
  quickAdd: (text: string) => invoke<Task>("quick_add", { text }),
  setStatus: (id: string, status: TaskStatus) => invoke<void>("set_task_status", { id, status }),
  reschedule: (id: string, date: string | null, time: string | null = null) =>
    invoke<void>("reschedule_task", { id, date, time }),
  deleteTask: (id: string) => invoke<void>("delete_task", { id }),
  addDependency: (taskId: string, blockedBy: string) =>
    invoke<void>("add_dependency", { taskId, blockedBy }),
  removeDependency: (taskId: string, blockedBy: string) =>
    invoke<void>("remove_dependency", { taskId, blockedBy }),
  runBacklogSweep: () => invoke<number>("run_backlog_sweep"),
  createCyclePattern: (name: string, anchorDate: string, slots: CycleSlot[]) =>
    invoke<number>("create_cycle_pattern", { name, anchorDate, slots }),
  listCyclePatterns: () => invoke<CyclePattern[]>("list_cycle_patterns"),
  cycleDays: (patternId: number, from: string, to: string) =>
    invoke<CycleDay[]>("cycle_days", { patternId, from, to }),
  setCycleOverride: (patternId: number, date: string, label: string | null) =>
    invoke<void>("set_cycle_override", { patternId, date, label }),
  pomodoroStart: (taskId: string | null, minutes: number) =>
    invoke<PomodoroTick>("pomodoro_start", { taskId, minutes }),
  pomodoroStop: () => invoke<void>("pomodoro_stop"),
  pomodoroStatus: () => invoke<PomodoroTick>("pomodoro_status"),
  setPipInteractive: (interactive: boolean) => invoke<void>("set_pip_interactive", { interactive }),
  hideQuickAdd: () => invoke<void>("hide_quick_add"),
  toggleQuickAdd: () => invoke<void>("toggle_quick_add"),
  showMain: () => invoke<void>("show_main"),
};

export const events = {
  tasksChanged: (cb: () => void): Promise<UnlistenFn> => listen("tasks:changed", () => cb()),
  pomodoroTick: (cb: (t: PomodoroTick) => void) => listen<PomodoroTick>("pomodoro:tick", (e) => cb(e.payload)),
  pipInteractive: (cb: (v: boolean) => void) => listen<boolean>("pip:interactive", (e) => cb(e.payload)),
};

export const ENERGY_LABEL: Record<EnergyLevel, string> = {
  high_focus: "고집중",
  low_routine: "단순반복",
  physical: "체력",
};

export const PRIORITY_COLOR = ["text-faint", "text-sky-400", "text-amber-400", "text-red-400"];

export function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function friendlyDate(iso: string | null): string {
  if (!iso) return "";
  const today = isoDate(new Date());
  const tmr = isoDate(new Date(Date.now() + 86_400_000));
  if (iso === today) return "오늘";
  if (iso === tmr) return "내일";
  const [, m, d] = iso.split("-").map(Number);
  const overdue = iso < today ? " (지남)" : "";
  return `${m}월 ${d}일${overdue}`;
}

export function fmtMinutes(min: number | null): string {
  if (!min) return "";
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h ? (m ? `${h}h${m}m` : `${h}h`) : `${m}m`;
}

export function fmtClock(secs: number): string {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}
