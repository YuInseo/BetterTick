import { create } from "zustand";
import { api, type CyclePattern, type PomodoroTick, type Task, type TaskView } from "./lib/api";

export type Section = TaskView | "cycle";

interface AppState {
  section: Section;
  tasks: Task[];
  selected: number;
  loading: boolean;
  error: string | null;
  patterns: CyclePattern[];
  pomodoro: PomodoroTick | null;
  setSection: (s: Section) => void;
  select: (i: number) => void;
  move: (delta: number) => void;
  refresh: () => Promise<void>;
  loadPatterns: () => Promise<void>;
  setPomodoro: (t: PomodoroTick) => void;
  setError: (e: string | null) => void;
}

export const useApp = create<AppState>((set, get) => ({
  section: "today",
  tasks: [],
  selected: 0,
  loading: false,
  error: null,
  patterns: [],
  pomodoro: null,
  setSection: (section) => {
    set({ section, selected: 0 });
    void get().refresh();
  },
  select: (selected) => set({ selected }),
  move: (delta) => {
    const { tasks, selected } = get();
    if (!tasks.length) return;
    set({ selected: Math.max(0, Math.min(tasks.length - 1, selected + delta)) });
  },
  refresh: async () => {
    const { section } = get();
    if (section === "cycle") {
      await get().loadPatterns();
      return;
    }
    set({ loading: true });
    try {
      const tasks = await api.listTasks(section);
      set((s) => ({ tasks, loading: false, selected: Math.min(s.selected, Math.max(0, tasks.length - 1)) }));
    } catch (e) {
      set({ loading: false, error: String(e) });
    }
  },
  loadPatterns: async () => {
    try {
      set({ patterns: await api.listCyclePatterns() });
    } catch (e) {
      set({ error: String(e) });
    }
  },
  setPomodoro: (pomodoro) => set({ pomodoro }),
  setError: (error) => set({ error }),
}));
