import { useEffect, useState } from "react";
import { Square } from "lucide-react";
import { api, events, fmtClock, type PomodoroTick } from "../lib/api";

const CATEGORY_COLOR = {
  focus: "bg-ok",
  neutral: "bg-faint",
  distraction: "bg-danger",
} as const;

/**
 * Always-on-top mini timer. Click-through by default; Ctrl+Alt+P (or the tray)
 * makes it interactive so it can be dragged or stopped.
 */
export default function Pip() {
  const [tick, setTick] = useState<PomodoroTick | null>(null);
  const [interactive, setInteractive] = useState(false);

  useEffect(() => {
    void api.pomodoroStatus().then(setTick);
    const a = events.pomodoroTick(setTick);
    const b = events.pipInteractive(setInteractive);
    return () => {
      void a.then((f) => f());
      void b.then((f) => f());
    };
  }, []);

  if (!tick?.running) return null;
  const progress = tick.total_secs ? 1 - tick.remaining_secs / tick.total_secs : 0;

  return (
    <div className="h-full p-1.5">
      <div
        data-tauri-drag-region
        className={`relative flex h-full items-center gap-3 overflow-hidden rounded-xl border px-3 backdrop-blur-xl transition-colors ${
          interactive ? "border-accent bg-panel/95" : "border-line/60 bg-panel/70"
        }`}
      >
        <div className="absolute inset-x-0 bottom-0 h-[3px] bg-line">
          <div className="h-full bg-accent transition-[width] duration-1000" style={{ width: `${progress * 100}%` }} />
        </div>
        <div data-tauri-drag-region className="min-w-0 flex-1">
          <div data-tauri-drag-region className="font-mono text-2xl font-semibold tabular-nums leading-none text-fg">
            {fmtClock(tick.remaining_secs)}
          </div>
          <div data-tauri-drag-region className="mt-1 flex items-center gap-1.5 truncate text-[11px] text-muted">
            {tick.current_category && (
              <span className={`size-1.5 shrink-0 rounded-full ${CATEGORY_COLOR[tick.current_category]}`} />
            )}
            <span className="truncate">{tick.task_title ?? tick.current_app ?? "집중"}</span>
          </div>
        </div>
        {interactive && (
          <button
            onClick={() => void api.pomodoroStop()}
            className="rounded-md p-1.5 text-muted hover:bg-raised hover:text-danger"
            title="정지"
          >
            <Square size={14} />
          </button>
        )}
      </div>
    </div>
  );
}
