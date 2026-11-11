import { useEffect, useRef, useState } from "react";
import { CalendarDays, Clock, Flag, Hash, Hourglass, Zap } from "lucide-react";
import { listen } from "@tauri-apps/api/event";
import { api, ENERGY_LABEL, fmtMinutes, friendlyDate, type ParsedQuickAdd } from "../lib/api";

const PRIORITY_NAME = ["", "낮음", "중간", "높음"];

/** Raycast-style frameless bar. Enter saves, Esc hides. */
export default function QuickAdd() {
  const [text, setText] = useState("");
  const [preview, setPreview] = useState<ParsedQuickAdd | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  // Rust emits this every time the bar is shown.
  useEffect(() => {
    const un = listen("quickadd:opened", () => {
      setText("");
      setPreview(null);
      setError(null);
      requestAnimationFrame(() => input.current?.focus());
    });
    input.current?.focus();
    return () => {
      void un.then((f) => f());
    };
  }, []);

  // Live parse preview.
  useEffect(() => {
    if (!text.trim()) {
      setPreview(null);
      return;
    }
    let alive = true;
    api.parseQuickAdd(text).then((p) => alive && setPreview(p));
    return () => {
      alive = false;
    };
  }, [text]);

  const submit = async () => {
    if (!text.trim() || saving) return;
    setSaving(true);
    try {
      await api.quickAdd(text);
      setText("");
      setPreview(null);
      setError(null);
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  const chips: { icon: React.ReactNode; label: string; tone?: string }[] = [];
  if (preview) {
    if (preview.due_date) chips.push({ icon: <CalendarDays size={13} />, label: friendlyDate(preview.due_date) });
    if (preview.due_time) chips.push({ icon: <Clock size={13} />, label: preview.due_time });
    preview.tags.forEach((t) => chips.push({ icon: <Hash size={13} />, label: t }));
    if (preview.priority)
      chips.push({ icon: <Flag size={13} />, label: PRIORITY_NAME[preview.priority], tone: "text-amber-400" });
    if (preview.estimated_minutes)
      chips.push({ icon: <Hourglass size={13} />, label: fmtMinutes(preview.estimated_minutes) });
    if (preview.energy_level)
      chips.push({ icon: <Zap size={13} />, label: ENERGY_LABEL[preview.energy_level], tone: "text-accent" });
  }

  return (
    <div className="flex h-full items-start justify-center p-3">
      <div className="w-full overflow-hidden rounded-2xl border border-line bg-panel/95 shadow-2xl shadow-black/60 backdrop-blur-xl">
        <div className="flex items-center gap-3 px-5 py-4">
          <span className="text-accent">＋</span>
          <input
            ref={input}
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") void submit();
              if (e.key === "Escape") void api.hideQuickAdd();
            }}
            placeholder="내일 15:00 백엔드 API 리팩토링 #개발 !1 ~2h @고집중"
            className="flex-1 bg-transparent text-[17px] text-fg placeholder:text-faint outline-none"
            spellCheck={false}
          />
        </div>
        {(preview || error) && (
          <div className="border-t border-line px-5 py-3">
            {preview && (
              <div className="flex flex-wrap items-center gap-2 text-[12px]">
                <span className="mr-1 truncate text-sm text-fg">{preview.title || "제목 없음"}</span>
                {chips.map((c, i) => (
                  <span
                    key={i}
                    className={`flex items-center gap-1 rounded-md border border-line bg-raised px-2 py-0.5 ${c.tone ?? "text-muted"}`}
                  >
                    {c.icon}
                    {c.label}
                  </span>
                ))}
              </div>
            )}
            {error && <p className="mt-2 text-xs text-danger">{error}</p>}
          </div>
        )}
        <div className="flex justify-between border-t border-line px-5 py-2 text-[11px] text-faint">
          <span>#태그 · !1 높음 · ~2h 예상 · @고집중/@단순/@체력</span>
          <span>
            <kbd className="text-muted">Enter</kbd> 저장 · <kbd className="text-muted">Esc</kbd> 닫기
          </span>
        </div>
      </div>
    </div>
  );
}
