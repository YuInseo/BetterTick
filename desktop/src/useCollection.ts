import { useCallback, useEffect, useRef, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  setDoc,
  type Firestore,
} from "firebase/firestore";
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
        setItems(snap.docs.map((d) => ({ ...d.data(), id: d.id }) as T)),
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
          await setDoc(doc(db, "users", userId, name, clean.id), clean);
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
