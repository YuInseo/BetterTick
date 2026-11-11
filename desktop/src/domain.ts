export type DateValue =
  | string
  | Date
  | { seconds: number; nanoseconds?: number }
  | { toDate(): Date }
  | null;
export type Task = {
  id: string;
  title: string;
  notes?: string;
  listId: string;
  dueDate?: DateValue;
  startDate?: string;
  dueTime?: string;
  duration?: number;
  isCompleted: boolean;
  priority: number;
  repeatRule?: string | null;
  repeatInterval?: number;
  repeatUntil?: string;
  repeatFromCompletion?: boolean;
  recurrenceDay?: number;
  recurrenceMonth?: number;
  repeatSource?: string;
  tagIds?: string[];
  createdAt?: DateValue;
  completedAt?: DateValue;
  deletedAt?: string | null;
  sortOrder?: number;
  pinned?: boolean;
  isNote?: boolean;
  attachments?: { id: string; name: string; mime: string; data: string }[];
  section?: string;
  reminderMinutes?: number[];
  subtasks?: { id: string; title: string; done: boolean }[];
};
export type TaskList = {
  id: string;
  name: string;
  color: string;
  icon?: string;
  sortOrder?: number;
  folder?: string;
  isArchived?: boolean;
};
export type Tag = { id: string; name: string; color: string };
export type Habit = {
  id: string;
  name: string;
  color: string;
  target: number;
  unit: string;
  targetDays: number[];
  isArchived: boolean;
};
export type HabitLog = {
  id: string;
  habitId: string;
  date: string;
  value: number;
};
export type FocusSession = {
  id: string;
  taskId: string;
  startedAt: string;
  endedAt: string;
  seconds: number;
};
export type SmartFilter = {
  id: string;
  name: string;
  priority: number;
  tagId: string;
  listId: string;
  dated: boolean;
};
export const dateKey = (d: Date) =>
  [
    d.getFullYear(),
    String(d.getMonth() + 1).padStart(2, "0"),
    String(d.getDate()).padStart(2, "0"),
  ].join("-");
export function asDate(value: DateValue | undefined): Date | null {
  if (!value) return null;
  const d =
    value instanceof Date
      ? value
      : typeof value === "object" && "toDate" in value
        ? value.toDate()
        : typeof value === "object" && "seconds" in value
          ? new Date(value.seconds * 1000)
          : new Date(value as string);
  return Number.isNaN(d.getTime()) ? null : d;
}
export const taskDate = (t: Task) => asDate(t.dueDate);
export function nextOccurrence(t: Task, now = new Date()): Task | null {
  const original = taskDate(t);
  if (!original || !t.repeatRule) return null;
  const d = t.repeatFromCompletion ? new Date(now) : new Date(original);
  const interval = Math.max(
    1,
    Math.min(365, Math.floor(t.repeatInterval || 1)),
  );
  const day = t.repeatFromCompletion
      ? d.getDate()
      : (t.recurrenceDay ?? original.getDate()),
    month = t.repeatFromCompletion
      ? d.getMonth()
      : (t.recurrenceMonth ?? original.getMonth());
  if (t.repeatRule === "DAILY") d.setDate(d.getDate() + interval);
  else if (t.repeatRule === "WEEKDAYS") {
    do {
      d.setDate(d.getDate() + 1);
    } while (d.getDay() === 0 || d.getDay() === 6);
  } else if (t.repeatRule === "WEEKLY") d.setDate(d.getDate() + 7 * interval);
  else if (t.repeatRule === "MONTHLY") {
    d.setDate(1);
    d.setMonth(d.getMonth() + interval);
    d.setDate(
      Math.min(day, new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate()),
    );
  } else if (t.repeatRule === "YEARLY") {
    d.setDate(1);
    d.setFullYear(d.getFullYear() + interval);
    d.setMonth(month);
    d.setDate(Math.min(day, new Date(d.getFullYear(), month + 1, 0).getDate()));
  } else return null;
  if (t.repeatUntil && dateKey(d) > t.repeatUntil) return null;
  const source = t.repeatSource ?? t.id;
  return {
    ...t,
    id: source + "__" + dateKey(d),
    repeatSource: source,
    recurrenceDay: day,
    recurrenceMonth: month,
    isCompleted: false,
    completedAt: null,
    deletedAt: null,
    dueDate: d.toISOString(),
    subtasks: t.subtasks?.map((s) => ({ ...s, done: false })),
    reminderMinutes: t.reminderMinutes ?? [],
    sortOrder: d.getTime(),
  };
}
export function parseQuickAdd(
  text: string,
  now = new Date(),
): {
  title: string;
  dueDate: string | null;
  priority?: number;
  tagNames: string[];
} {
  let title = text.trim();
  let due: Date | null = null;
  let priority: number | undefined;
  const tagNames: Array<string> = [];
  title = title.replace(/(?:^|\s)#([^\s#]+)/g, (_, name) => {
    tagNames.push(name);
    return " ";
  });
  title = title.replace(/(?:^|\s)!([0-3])(?=\s|$)/g, (_, p) => {
    priority = Number(p);
    return " ";
  });
  title = title.replace(
    /(?:^|\s)(오늘|내일|모레|today|tomorrow|\d{4}-\d{2}-\d{2})(?=\s|$)/gi,
    (_, token: string) => {
      const d = new Date(now);
      d.setHours(12, 0, 0, 0);
      if (/^(내일|tomorrow)$/i.test(token)) d.setDate(d.getDate() + 1);
      else if (token === "모레") d.setDate(d.getDate() + 2);
      else if (/^\d{4}-/.test(token)) {
        const candidate = new Date(token + "T12:00:00");
        if (Number.isNaN(candidate.getTime()) || dateKey(candidate) !== token)
          return " " + token;
        due = candidate;
        return " ";
      }
      due = d;
      return " ";
    },
  );
  return {
    title: title.replace(/\s+/g, " ").trim(),
    dueDate: (due as Date | null)?.toISOString() ?? null,
    priority,
    tagNames,
  };
}
export function habitStreak(
  h: Habit,
  logs: HabitLog[],
  now = new Date(),
): number {
  let count = 0;
  const d = new Date(now);
  let first = true;
  for (let i = 0; i < 3660; i++, d.setDate(d.getDate() - 1)) {
    if (!h.targetDays.includes(d.getDay())) continue;
    const done = logs.some(
      (l) => l.habitId === h.id && l.date === dateKey(d) && l.value >= h.target,
    );
    if (!done) {
      if (first) {
        first = false;
        continue;
      }
      break;
    }
    count++;
    first = false;
  }
  return count;
}
export function validateBackup(input: unknown) {
  if (!input || typeof input !== "object") throw Error("잘못된 백업 형식");
  const p = input as Record<string, unknown>;
  if (
    p.format !== "bettertick-backup" ||
    p.version !== 1 ||
    !Array.isArray(p.tasks) ||
    !Array.isArray(p.lists)
  )
    throw Error("지원하지 않는 백업 파일");
  const validId = (id: unknown) =>
    typeof id === "string" &&
    id.length > 0 &&
    id.length < 1000 &&
    !id.includes("/");
  const arrays = [
    "tasks",
    "lists",
    "tags",
    "habits",
    "habitLogs",
    "sessions",
    "filters",
    "countdowns",
  ];
  for (const name of arrays) {
    if (p[name] === undefined) continue;
    if (!Array.isArray(p[name])) throw Error(name + " 형식 오류");
    for (const item of p[name] as Record<string, unknown>[]) {
      if (!item || typeof item !== "object" || !validId(item.id))
        throw Error(name + " ID 오류");
    }
  }
  for (const t of p.tasks as Task[])
    if (
      typeof t.title !== "string" ||
      typeof t.listId !== "string" ||
      typeof t.isCompleted !== "boolean" ||
      !Number.isInteger(t.priority) ||
      t.priority < 0 ||
      t.priority > 3 ||
      (t.dueDate && !asDate(t.dueDate)) ||
      (t.notes !== undefined && typeof t.notes !== "string") ||
      (t.startDate !== undefined &&
        t.startDate !== "" &&
        (!/^\d{4}-\d{2}-\d{2}$/.test(t.startDate) || !asDate(t.startDate))) ||
      (t.dueTime !== undefined &&
        t.dueTime !== "" &&
        !/^([01]\d|2[0-3]):[0-5]\d$/.test(t.dueTime)) ||
      (t.repeatInterval !== undefined &&
        (!Number.isInteger(t.repeatInterval) ||
          t.repeatInterval < 1 ||
          t.repeatInterval > 365)) ||
      (t.reminderMinutes !== undefined &&
        (!Array.isArray(t.reminderMinutes) ||
          t.reminderMinutes.some((m) => !Number.isFinite(m) || m < 0))) ||
      (t.subtasks !== undefined &&
        (!Array.isArray(t.subtasks) ||
          t.subtasks.some(
            (s) =>
              !s ||
              !validId(s.id) ||
              typeof s.title !== "string" ||
              typeof s.done !== "boolean",
          ))) ||
      (t.tagIds !== undefined &&
        (!Array.isArray(t.tagIds) || t.tagIds.some((id) => !validId(id))))
    )
      throw Error("할 일 데이터 오류");
  for (const l of p.lists as TaskList[])
    if (typeof l.name !== "string" || typeof l.color !== "string")
      throw Error("목록 데이터 오류");
  for (const t of (p.tags ?? []) as Tag[])
    if (typeof t.name !== "string" || typeof t.color !== "string")
      throw Error("태그 데이터 오류");
  for (const h of (p.habits ?? []) as Habit[])
    if (
      typeof h.name !== "string" ||
      !Number.isFinite(h.target) ||
      h.target <= 0 ||
      !Array.isArray(h.targetDays) ||
      h.targetDays.some((d) => !Number.isInteger(d) || d < 0 || d > 6)
    )
      throw Error("습관 데이터 오류");
  for (const l of (p.habitLogs ?? []) as HabitLog[])
    if (
      typeof l.habitId !== "string" ||
      !/^\d{4}-\d{2}-\d{2}$/.test(l.date) ||
      !Number.isFinite(l.value) ||
      l.value < 0
    )
      throw Error("습관 기록 오류");
  for (const s of (p.sessions ?? []) as FocusSession[])
    if (
      typeof s.taskId !== "string" ||
      !asDate(s.startedAt) ||
      !asDate(s.endedAt) ||
      !Number.isFinite(s.seconds) ||
      s.seconds < 0
    )
      throw Error("집중 기록 오류");
  for (const c of (p.countdowns ?? []) as {
    name: unknown;
    date: string;
    color: unknown;
    yearly: unknown;
  }[])
    if (
      typeof c.name !== "string" ||
      typeof c.color !== "string" ||
      !/^\d{4}-\d{2}-\d{2}$/.test(c.date) ||
      !asDate(c.date) ||
      typeof c.yearly !== "boolean"
    )
      throw Error("디데이 데이터 오류");
  for (const f of (p.filters ?? []) as SmartFilter[])
    if (
      typeof f.name !== "string" ||
      !Number.isInteger(f.priority) ||
      f.priority < -1 ||
      f.priority > 3 ||
      typeof f.tagId !== "string" ||
      typeof f.listId !== "string" ||
      typeof f.dated !== "boolean"
    )
      throw Error("필터 데이터 오류");
  for (const t of p.tasks as Task[])
    if (
      t.attachments &&
      (!Array.isArray(t.attachments) ||
        t.attachments.some(
          (a) =>
            !a ||
            !validId(a.id) ||
            typeof a.name !== "string" ||
            typeof a.data !== "string" ||
            !/^data:[\w.+/-]+;base64,[A-Za-z0-9+/=]+$/.test(a.data) ||
            a.data.length > 1500000,
        ))
    )
      throw Error("첨부 파일 오류");
  return p as unknown as {
    format: string;
    version: number;
    tasks: Task[];
    lists: TaskList[];
    tags?: Tag[];
    habits?: Habit[];
    habitLogs?: HabitLog[];
    sessions?: FocusSession[];
    filters?: SmartFilter[];
    countdowns?: {
      id: string;
      name: string;
      date: string;
      color: string;
      yearly: boolean;
    }[];
    preferences?: unknown;
  };
}
