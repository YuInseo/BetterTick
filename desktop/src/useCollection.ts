import { useCallback, useEffect, useRef, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  setDoc,
  type Firestore,
  Timestamp,
} from "firebase/firestore";
import { asDate } from "./domain";
export function useCollection<T extends { id: string }>(
  db: Firestore | null,
  userId: string,
  name: string,
  onError: (message: string) => void,
) {
  const key = `bettertick.desktop.data.v1.${userId}.${name}`;
  const read = () => {
    try {
      const value = JSON.parse(localStorage.getItem(key) || "[]");
      return Array.isArray(value) ? (value as T[]) : [];
    } catch {
      return [];
    }
  };
  const [items, setItems] = useState<T[]>(read);
  const savedKey = useRef(key);
  const loadedKey = useRef(key);
  useEffect(() => {
    if (loadedKey.current !== key) {
      loadedKey.current = key;
      setItems(read());
    }
    if (!db) return;
    return onSnapshot(
      collection(db, "users", userId, name),
      (snap) =>
        setItems(
          snap.docs.map((d) => {
            const raw = { ...d.data(), id: d.id } as Record<string, unknown>;
            if (name === "habits") {
              raw.target = raw.target ?? 1;
              raw.unit = raw.unit ?? "회";
              raw.targetDays = Array.isArray(raw.targetDays)
                ? raw.targetDays.map((day) => Number(day) % 7)
                : [0, 1, 2, 3, 4, 5, 6];
              raw.isArchived = raw.isArchived ?? false;
            }
            if (name === "habitLogs")
              raw.value = raw.value ?? (raw.isCompleted ? 1 : 0);
            if (name === "tasks") {
              raw.desktopAttachments = raw.desktopAttachments ?? [];
              raw.pinned = raw.pinned ?? raw.isPinned ?? false;
              raw.reminderMinutes = raw.reminderMinutes ?? raw.reminders ?? [];
              raw.duration = raw.duration ?? raw.durationMinutes ?? 0;
            }
            return raw as T;
          }),
        ),
      (e) => onError(e.message),
    );
  }, [db, userId, name]);
  useEffect(() => {
    if (savedKey.current !== key) {
      savedKey.current = key;
      return;
    }
    try {
      localStorage.setItem(key, JSON.stringify(items));
    } catch {
      onError("저장 공간이 부족합니다. 백업을 내보내세요.");
    }
  }, [items, key]);
  const put = useCallback(
    async (item: T) => {
      // Strip undefined fields and normalize Dates to portable ISO strings.
      const clean = JSON.parse(JSON.stringify(item)) as T;
      setItems((old) => {
        const next = [...old.filter((x) => x.id !== clean.id), clean];
        return next;
      });
      if (db)
        try {
          const remote = JSON.parse(JSON.stringify(clean)) as Record<
            string,
            unknown
          >;
          for (const field of ["dueDate", "createdAt", "completedAt"]) {
            const value = asDate(remote[field] as Parameters<typeof asDate>[0]);
            if (value) remote[field] = Timestamp.fromDate(value);
          }
          if (name === "habits" && Array.isArray(remote.targetDays))
            remote.targetDays = remote.targetDays.map((d) => (d === 0 ? 7 : d));
          if (name === "tasks") {
            remote.updatedAt = Timestamp.now();
            if (remote.pinned !== undefined) remote.isPinned = remote.pinned;
            if (remote.reminderMinutes !== undefined)
              remote.reminders = remote.reminderMinutes;
            if (remote.duration !== undefined)
              remote.durationMinutes = remote.duration;
          }
          await setDoc(doc(db, "users", userId, name, clean.id), remote, {
            merge: true,
          });
        } catch (e) {
          onError("동기화 실패: " + String(e));
        }
    },
    [db, userId, name],
  );
  const remove = useCallback(
    async (id: string) => {
      setItems((old) => old.filter((x) => x.id !== id));
      if (db)
        try {
          await deleteDoc(doc(db, "users", userId, name, id));
        } catch (e) {
          onError("삭제 실패: " + String(e));
        }
    },
    [db, userId, name],
  );
  return { items, setItems, put, remove };
}
