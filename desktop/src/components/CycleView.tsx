import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import { api, isoDate, type CycleDay } from "../lib/api";
import { useApp } from "../store";

const PALETTE = ["#5e6ad2", "#8b5cf6", "#4cb782", "#f2994a", "#eb5757", "#26b5ce"];

/** N-day rotation editor + month calendar showing each day's cycle state. */
export default function CycleView() {
  const { patterns, loadPatterns, setError } = useApp();
  const [activeId, setActiveId] = useState<number | null>(null);
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  });
  const [days, setDays] = useState<CycleDay[]>([]);

  const [name, setName] = useState("교대 근무");
  const [anchor, setAnchor] = useState(isoDate(new Date()));
  const [slots, setSlots] = useState("주간, 야간, 비번, 휴무");

  useEffect(() => {
    void loadPatterns();
  }, [loadPatterns]);
  useEffect(() => {
    if (activeId === null && patterns.length) setActiveId(patterns[0].id);
  }, [patterns, activeId]);

  const active = patterns.find((p) => p.id === activeId) ?? null;

  // Calendar grid: Sunday-start weeks covering the month.
  const grid = useMemo(() => {
    const start = new Date(month);
    start.setDate(1 - start.getDay());
    return Array.from({ length: 42 }, (_, i) => new Date(start.getFullYear(), start.getMonth(), start.getDate() + i));
  }, [month]);

  const reload = () => {
    if (!active) return setDays([]);
    api
      .cycleDays(active.id, isoDate(grid[0]), isoDate(grid[41]))
      .then(setDays)
      .catch((e) => setError(String(e)));
  };
  useEffect(reload, [active?.id, grid]); // eslint-disable-line react-hooks/exhaustive-deps

  const byDate = new Map(days.map((d) => [d.date, d]));
  const today = isoDate(new Date());

  const create = async () => {
    const labels = slots.split(",").map((s) => s.trim()).filter(Boolean);
    if (!name.trim() || !labels.length) return;
    try {
      const id = await api.createCyclePattern(
        name,
        anchor,
        labels.map((label, i) => ({ label, color: PALETTE[i % PALETTE.length] })),
      );
      await loadPatterns();
      setActiveId(id);
    } catch (e) {
      setError(String(e));
    }
  };

  // Click: next label as an override. Shift+click: back to the rotation.
  const cycleOverride = async (date: string, current: CycleDay | undefined, clear: boolean) => {
    if (!active) return;
    const labels = active.slots.map((s) => s.label);
    const next = clear ? null : labels[(labels.indexOf(current?.label ?? "") + 1) % labels.length];
    await api.setCycleOverride(active.id, date, next);
    reload();
  };

  return (
    <div className="flex h-full gap-6">
      <div className="w-64 shrink-0 space-y-4">
        <div className="space-y-1">
          {patterns.map((p) => (
            <button
              key={p.id}
              onClick={() => setActiveId(p.id)}
              className={`w-full rounded-lg px-3 py-2 text-left text-sm ${
                p.id === activeId ? "bg-raised text-fg ring-1 ring-line" : "text-muted hover:bg-raised/60"
              }`}
            >
              <div>{p.name}</div>
              <div className="text-[11px] text-faint">
                {p.slots.length}일 주기 · {p.slots.map((s) => s.label).join(" → ")}
              </div>
            </button>
          ))}
        </div>

        <div className="space-y-2 rounded-xl border border-line bg-panel p-3 text-sm">
          <div className="text-xs font-medium text-muted">새 주기</div>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-md border border-line bg-bg px-2 py-1.5 outline-none focus:border-accent"
            placeholder="이름"
          />
          <label className="block text-[11px] text-faint">
            기준일 (첫 칸이 되는 날)
            <input
              type="date"
              value={anchor}
              onChange={(e) => setAnchor(e.target.value)}
              className="mt-1 w-full rounded-md border border-line bg-bg px-2 py-1.5 text-fg outline-none focus:border-accent"
            />
          </label>
          <input
            value={slots}
            onChange={(e) => setSlots(e.target.value)}
            className="w-full rounded-md border border-line bg-bg px-2 py-1.5 outline-none focus:border-accent"
            placeholder="주간, 야간, 비번, 휴무"
          />
          <button
            onClick={() => void create()}
            className="flex w-full items-center justify-center gap-1 rounded-md bg-accent py-1.5 text-white hover:opacity-90"
          >
            <Plus size={14} /> 만들기
          </button>
        </div>
      </div>

      <div className="min-w-0 flex-1">
        <div className="mb-3 flex items-center justify-between">
          <div className="text-lg font-semibold">
            {month.getFullYear()}년 {month.getMonth() + 1}월
          </div>
          <div className="flex gap-1">
            <button
              onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}
              className="rounded-md p-1.5 text-muted hover:bg-raised"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}
              className="rounded-md p-1.5 text-muted hover:bg-raised"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
        {!active ? (
          <p className="text-sm text-faint">왼쪽에서 주기를 만들면 달력에 날마다 상태가 표시돼요.</p>
        ) : (
          <>
            <div className="grid grid-cols-7 gap-px overflow-hidden rounded-xl border border-line bg-line text-sm">
              {["일", "월", "화", "수", "목", "금", "토"].map((w) => (
                <div key={w} className="bg-panel py-1.5 text-center text-[11px] text-faint">
                  {w}
                </div>
              ))}
              {grid.map((d) => {
                const key = isoDate(d);
                const info = byDate.get(key);
                const inMonth = d.getMonth() === month.getMonth();
                return (
                  <button
                    key={key}
                    onClick={(e) => void cycleOverride(key, info, e.shiftKey)}
                    className={`flex h-20 flex-col items-start gap-1 bg-panel p-2 text-left hover:bg-raised ${
                      inMonth ? "" : "opacity-40"
                    }`}
                  >
                    <span className={`text-xs ${key === today ? "font-bold text-accent" : "text-muted"}`}>
                      {d.getDate()}
                    </span>
                    {info && (
                      <span
                        className="rounded px-1.5 py-0.5 text-[11px] font-medium text-white"
                        style={{ background: info.color ?? "#5e6ad2" }}
                      >
                        {info.label}
                        {info.overridden && " *"}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
            <p className="mt-2 text-[11px] text-faint">
              날짜 클릭: 그날만 다음 상태로 바꾸기 · Shift+클릭: 원래 주기로 되돌리기 (* = 바꾼 날)
            </p>
          </>
        )}
      </div>
    </div>
  );
}
