//! SQLite local-first store.
//!
//! - WAL journal, foreign keys on, versioned migrations via `PRAGMA user_version`.
//! - Covers: N-day cycle patterns (Feature B), task DAG dependencies + energy level
//!   + estimates + backlog quarantine (Feature C), pomodoro sessions + active-window
//!   focus samples, markdown sync bookkeeping (Feature D), habits.

use std::collections::{HashMap, HashSet};
use std::path::Path;
use std::sync::Mutex;

use chrono::{Local, NaiveDate, Utc};
use rusqlite::{params, Connection, OptionalExtension, Row};
use serde::{Deserialize, Serialize};

use crate::cycle::CyclePattern;

#[derive(Debug, thiserror::Error)]
pub enum DbError {
    #[error("sqlite: {0}")]
    Sqlite(#[from] rusqlite::Error),
    #[error("{0}")]
    Invalid(String),
}

impl Serialize for DbError {
    fn serialize<S: serde::Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        s.serialize_str(&self.to_string())
    }
}

pub type DbResult<T> = Result<T, DbError>;

/// Schema migrations, applied in order. Index + 1 == resulting `user_version`.
const MIGRATIONS: &[&str] = &[
    // ── v1 ────────────────────────────────────────────────────────────────
    r#"
    -- N-day rotation (e.g. 주간 → 야간 → 비번 → 휴무). Slot 0 falls on anchor_date.
    CREATE TABLE cycle_patterns (
        id           INTEGER PRIMARY KEY,
        name         TEXT    NOT NULL UNIQUE,
        anchor_date  TEXT    NOT NULL CHECK (date(anchor_date) IS NOT NULL),
        length       INTEGER NOT NULL CHECK (length BETWEEN 1 AND 366),
        is_active    INTEGER NOT NULL DEFAULT 1,
        created_at   TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
    );
    CREATE TABLE cycle_slots (
        pattern_id   INTEGER NOT NULL REFERENCES cycle_patterns(id) ON DELETE CASCADE,
        slot_index   INTEGER NOT NULL CHECK (slot_index >= 0),
        label        TEXT    NOT NULL,
        color        TEXT,
        PRIMARY KEY (pattern_id, slot_index)
    ) WITHOUT ROWID;
    -- One-off swaps (근무 바꾸기) without breaking the rotation.
    CREATE TABLE cycle_overrides (
        pattern_id   INTEGER NOT NULL REFERENCES cycle_patterns(id) ON DELETE CASCADE,
        date         TEXT    NOT NULL CHECK (date(date) IS NOT NULL),
        label        TEXT    NOT NULL,
        PRIMARY KEY (pattern_id, date)
    ) WITHOUT ROWID;

    CREATE TABLE tags (
        id     INTEGER PRIMARY KEY,
        name   TEXT NOT NULL UNIQUE COLLATE NOCASE,
        color  TEXT
    );

    CREATE TABLE tasks (
        id                 TEXT    PRIMARY KEY,
        title              TEXT    NOT NULL CHECK (length(trim(title)) > 0),
        notes              TEXT    NOT NULL DEFAULT '',
        status             TEXT    NOT NULL DEFAULT 'todo'
                                   CHECK (status IN ('todo','done','cancelled','backlog')),
        priority           INTEGER NOT NULL DEFAULT 0 CHECK (priority BETWEEN 0 AND 3),
        due_date           TEXT    CHECK (due_date IS NULL OR date(due_date) IS NOT NULL),
        due_time           TEXT    CHECK (due_time IS NULL OR time(due_time) IS NOT NULL),
        energy_level       TEXT    CHECK (energy_level IN ('high_focus','low_routine','physical')),
        estimated_minutes  INTEGER CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
        -- Cycle binding: only surfaced on days whose cycle label matches.
        cycle_pattern_id   INTEGER REFERENCES cycle_patterns(id) ON DELETE SET NULL,
        cycle_label        TEXT,
        -- Todo-bankruptcy guard.
        postpone_count     INTEGER NOT NULL DEFAULT 0,
        first_due_date     TEXT,
        backlogged_at      TEXT,
        completed_at       TEXT,
        source             TEXT    NOT NULL DEFAULT 'app' CHECK (source IN ('app','quickadd','markdown')),
        sort_order         REAL    NOT NULL DEFAULT 0,
        created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
        updated_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
        CHECK (cycle_label IS NULL OR cycle_pattern_id IS NOT NULL)
    );
    CREATE INDEX idx_tasks_status_due ON tasks(status, due_date);
    CREATE INDEX idx_tasks_cycle ON tasks(cycle_pattern_id, cycle_label) WHERE cycle_pattern_id IS NOT NULL;

    CREATE TABLE task_tags (
        task_id  TEXT    NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
        tag_id   INTEGER NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
        PRIMARY KEY (task_id, tag_id)
    ) WITHOUT ROWID;

    -- DAG edges: task_id is blocked until blocked_by is done. Cycles are
    -- rejected in Rust (add_dependency) before insert.
    CREATE TABLE task_dependencies (
        task_id     TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
        blocked_by  TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
        PRIMARY KEY (task_id, blocked_by),
        CHECK (task_id <> blocked_by)
    ) WITHOUT ROWID;
    CREATE INDEX idx_dep_blocker ON task_dependencies(blocked_by);

    -- Open blockers per task (a blocker counts until it is done or cancelled).
    CREATE VIEW v_task_open_blockers AS
        SELECT d.task_id, COUNT(*) AS open_blockers
        FROM task_dependencies d
        JOIN tasks b ON b.id = d.blocked_by
        WHERE b.status NOT IN ('done','cancelled')
        GROUP BY d.task_id;

    CREATE TABLE habits (
        id                INTEGER PRIMARY KEY,
        name              TEXT    NOT NULL,
        energy_level      TEXT    CHECK (energy_level IN ('high_focus','low_routine','physical')),
        target_per_day    INTEGER NOT NULL DEFAULT 1 CHECK (target_per_day > 0),
        cycle_pattern_id  INTEGER REFERENCES cycle_patterns(id) ON DELETE SET NULL,
        cycle_label       TEXT,
        archived          INTEGER NOT NULL DEFAULT 0,
        created_at        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
    );
    CREATE TABLE habit_logs (
        habit_id  INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
        date      TEXT    NOT NULL CHECK (date(date) IS NOT NULL),
        count     INTEGER NOT NULL DEFAULT 1 CHECK (count >= 0),
        PRIMARY KEY (habit_id, date)
    ) WITHOUT ROWID;

    CREATE TABLE pomodoro_sessions (
        id               INTEGER PRIMARY KEY,
        task_id          TEXT    REFERENCES tasks(id) ON DELETE SET NULL,
        planned_minutes  INTEGER NOT NULL CHECK (planned_minutes > 0),
        started_at       TEXT    NOT NULL,
        ended_at         TEXT,
        status           TEXT    NOT NULL DEFAULT 'running'
                                 CHECK (status IN ('running','completed','aborted')),
        focus_score      REAL    CHECK (focus_score IS NULL OR focus_score BETWEEN 0 AND 1)
    );
    CREATE INDEX idx_pomo_task ON pomodoro_sessions(task_id);

    -- Active-window samples taken while a session runs.
    CREATE TABLE focus_samples (
        id            INTEGER PRIMARY KEY,
        session_id    INTEGER NOT NULL REFERENCES pomodoro_sessions(id) ON DELETE CASCADE,
        sampled_at    TEXT    NOT NULL,
        process_name  TEXT    NOT NULL,
        window_title  TEXT    NOT NULL DEFAULT '',
        category      TEXT    NOT NULL CHECK (category IN ('focus','neutral','distraction'))
    );
    CREATE INDEX idx_focus_session ON focus_samples(session_id);

    -- Markdown two-way sync (Step 2 engine uses these).
    CREATE TABLE md_sources (
        id              INTEGER PRIMARY KEY,
        path            TEXT NOT NULL UNIQUE,
        last_synced_at  TEXT,
        content_hash    TEXT
    );
    CREATE TABLE md_task_links (
        task_id    TEXT    PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE,
        source_id  INTEGER NOT NULL REFERENCES md_sources(id) ON DELETE CASCADE,
        line_no    INTEGER NOT NULL,
        line_hash  TEXT    NOT NULL
    );

    CREATE TRIGGER trg_tasks_touch AFTER UPDATE ON tasks
    WHEN NEW.updated_at = OLD.updated_at
    BEGIN
        UPDATE tasks SET updated_at = strftime('%Y-%m-%dT%H:%M:%SZ','now') WHERE id = NEW.id;
    END;
    "#,
];

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum EnergyLevel {
    HighFocus,
    LowRoutine,
    Physical,
}

impl EnergyLevel {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::HighFocus => "high_focus",
            Self::LowRoutine => "low_routine",
            Self::Physical => "physical",
        }
    }
    fn parse(s: &str) -> Option<Self> {
        match s {
            "high_focus" => Some(Self::HighFocus),
            "low_routine" => Some(Self::LowRoutine),
            "physical" => Some(Self::Physical),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum TaskStatus {
    Todo,
    Done,
    Cancelled,
    Backlog,
}

impl TaskStatus {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Todo => "todo",
            Self::Done => "done",
            Self::Cancelled => "cancelled",
            Self::Backlog => "backlog",
        }
    }
    fn parse(s: &str) -> Self {
        match s {
            "done" => Self::Done,
            "cancelled" => Self::Cancelled,
            "backlog" => Self::Backlog,
            _ => Self::Todo,
        }
    }
}

#[derive(Debug, Clone, Serialize)]
pub struct Task {
    pub id: String,
    pub title: String,
    pub notes: String,
    pub status: TaskStatus,
    pub priority: u8,
    pub due_date: Option<NaiveDate>,
    pub due_time: Option<String>,
    pub energy_level: Option<EnergyLevel>,
    pub estimated_minutes: Option<u32>,
    pub cycle_pattern_id: Option<i64>,
    pub cycle_label: Option<String>,
    pub postpone_count: u32,
    pub tags: Vec<String>,
    pub blocked_by: Vec<String>,
    /// True while any `blocked_by` task is still open.
    pub is_blocked: bool,
    pub created_at: String,
    pub completed_at: Option<String>,
}

#[derive(Debug, Clone, Default, Deserialize)]
pub struct NewTask {
    pub title: String,
    #[serde(default)]
    pub notes: String,
    #[serde(default)]
    pub priority: u8,
    pub due_date: Option<NaiveDate>,
    pub due_time: Option<String>,
    pub energy_level: Option<EnergyLevel>,
    pub estimated_minutes: Option<u32>,
    pub cycle_pattern_id: Option<i64>,
    pub cycle_label: Option<String>,
    #[serde(default)]
    pub tags: Vec<String>,
    #[serde(default)]
    pub blocked_by: Vec<String>,
    #[serde(default)]
    pub source: Option<String>,
}

#[derive(Debug, Clone, Copy, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum TaskView {
    /// Open tasks due today or overdue, plus undated ones bound to today's cycle label.
    Today,
    /// Open tasks due within the next 7 days.
    Upcoming,
    /// Every open (todo) task.
    All,
    Backlog,
    Done,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CycleSlotInput {
    pub label: String,
    pub color: Option<String>,
}

#[derive(Debug, Clone, Serialize)]
pub struct CyclePatternRow {
    pub id: i64,
    pub name: String,
    pub anchor_date: NaiveDate,
    pub slots: Vec<CycleSlotInput>,
    pub is_active: bool,
}

#[derive(Debug, Clone, Serialize)]
pub struct CycleDay {
    pub date: NaiveDate,
    pub label: String,
    pub color: Option<String>,
    pub overridden: bool,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum FocusCategory {
    Focus,
    Neutral,
    Distraction,
}

impl FocusCategory {
    fn as_str(self) -> &'static str {
        match self {
            Self::Focus => "focus",
            Self::Neutral => "neutral",
            Self::Distraction => "distraction",
        }
    }
}

pub struct Db {
    conn: Mutex<Connection>,
}

fn now_iso() -> String {
    Utc::now().format("%Y-%m-%dT%H:%M:%SZ").to_string()
}

impl Db {
    /// Opens (or creates) the database file and applies pending migrations.
    pub fn open(path: &Path) -> DbResult<Self> {
        if let Some(dir) = path.parent() {
            std::fs::create_dir_all(dir).map_err(|e| DbError::Invalid(e.to_string()))?;
        }
        Self::init(Connection::open(path)?)
    }

    /// In-memory database (tests).
    pub fn open_in_memory() -> DbResult<Self> {
        Self::init(Connection::open_in_memory()?)
    }

    fn init(conn: Connection) -> DbResult<Self> {
        // WAL: readers never block the writer; NORMAL sync is safe under WAL.
        conn.pragma_update(None, "journal_mode", "WAL")?;
        conn.pragma_update(None, "synchronous", "NORMAL")?;
        conn.pragma_update(None, "foreign_keys", "ON")?;
        conn.pragma_update(None, "busy_timeout", 5000)?;
        migrate(&conn)?;
        Ok(Self { conn: Mutex::new(conn) })
    }

    fn with<T>(&self, f: impl FnOnce(&mut Connection) -> DbResult<T>) -> DbResult<T> {
        let mut guard = self.conn.lock().map_err(|_| DbError::Invalid("db lock poisoned".into()))?;
        f(&mut guard)
    }

    // ── Tasks ────────────────────────────────────────────────────────────

    pub fn insert_task(&self, t: NewTask) -> DbResult<Task> {
        let title = t.title.trim().to_string();
        if title.is_empty() {
            return Err(DbError::Invalid("제목이 비어 있어요".into()));
        }
        if t.priority > 3 {
            return Err(DbError::Invalid("priority must be 0..=3".into()));
        }
        let id = uuid::Uuid::new_v4().to_string();
        self.with(|c| {
            let tx = c.transaction()?;
            let due = t.due_date.map(|d| d.to_string());
            tx.execute(
                "INSERT INTO tasks (id, title, notes, priority, due_date, due_time, energy_level,
                    estimated_minutes, cycle_pattern_id, cycle_label, first_due_date, source)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?5, ?11)",
                params![
                    id,
                    title,
                    t.notes,
                    t.priority,
                    due,
                    t.due_time,
                    t.energy_level.map(|e| e.as_str()),
                    t.estimated_minutes,
                    t.cycle_pattern_id,
                    t.cycle_label,
                    t.source.as_deref().unwrap_or("app"),
                ],
            )?;
            for tag in &t.tags {
                attach_tag(&tx, &id, tag)?;
            }
            for blocker in &t.blocked_by {
                add_dependency_tx(&tx, &id, blocker)?;
            }
            tx.commit()?;
            Ok(())
        })?;
        self.get_task(&id)?.ok_or_else(|| DbError::Invalid("insert lost".into()))
    }

    pub fn get_task(&self, id: &str) -> DbResult<Option<Task>> {
        self.with(|c| {
            let task = c
                .query_row(&format!("{TASK_SELECT} WHERE t.id = ?1"), [id], task_from_row)
                .optional()?;
            match task {
                Some(t) => Ok(hydrate(c, vec![t])?.into_iter().next()),
                None => Ok(None),
            }
        })
    }

    pub fn list_tasks(&self, view: TaskView, today: NaiveDate) -> DbResult<Vec<Task>> {
        let today_s = today.to_string();
        let week_s = (today + chrono::Duration::days(7)).to_string();
        let (filter, order) = match view {
            TaskView::Today => (
                "t.status = 'todo' AND (t.due_date <= ?1 OR (t.due_date IS NULL AND t.cycle_pattern_id IS NOT NULL))",
                "t.due_date IS NULL, t.due_date, t.due_time IS NULL, t.due_time, t.priority DESC",
            ),
            TaskView::Upcoming => (
                "t.status = 'todo' AND t.due_date > ?1 AND t.due_date <= ?2",
                "t.due_date, t.due_time IS NULL, t.due_time, t.priority DESC",
            ),
            TaskView::All => (
                "t.status = 'todo'",
                "t.due_date IS NULL, t.due_date, t.due_time IS NULL, t.due_time, t.priority DESC",
            ),
            TaskView::Backlog => ("t.status = 'backlog'", "t.backlogged_at DESC"),
            TaskView::Done => (
                "t.status IN ('done','cancelled') AND t.completed_at >= date(?1, '-14 days')",
                "t.completed_at DESC",
            ),
        };
        let sql = format!("{TASK_SELECT} WHERE {filter} ORDER BY {order}");
        let tasks = self.with(|c| {
            let mut stmt = c.prepare(&sql)?;
            // Bind only the parameters the filter uses.
            let n = stmt.parameter_count();
            let rows = match n {
                0 => stmt.query_map([], task_from_row)?.collect::<Result<Vec<_>, _>>()?,
                1 => stmt.query_map([&today_s], task_from_row)?.collect::<Result<Vec<_>, _>>()?,
                _ => stmt
                    .query_map([&today_s, &week_s], task_from_row)?
                    .collect::<Result<Vec<_>, _>>()?,
            };
            hydrate(c, rows)
        })?;
        if view != TaskView::Today {
            return Ok(tasks);
        }
        // Cycle-bound undated tasks only show when today's label matches.
        let labels = self.cycle_labels_on(today)?;
        Ok(tasks
            .into_iter()
            .filter(|t| match (t.due_date, t.cycle_pattern_id, &t.cycle_label) {
                (None, Some(pid), Some(label)) => labels.get(&pid) == Some(label),
                (None, Some(_), None) => true,
                _ => true,
            })
            .collect())
    }

    pub fn set_status(&self, id: &str, status: TaskStatus) -> DbResult<()> {
        self.with(|c| {
            let completed = matches!(status, TaskStatus::Done | TaskStatus::Cancelled).then(now_iso);
            let n = c.execute(
                "UPDATE tasks SET status = ?2, completed_at = ?3,
                    backlogged_at = CASE WHEN ?2 = 'backlog' THEN ?4 ELSE NULL END
                 WHERE id = ?1",
                params![id, status.as_str(), completed, now_iso()],
            )?;
            if n == 0 {
                return Err(DbError::Invalid(format!("task {id} not found")));
            }
            Ok(())
        })
    }

    /// Moves a task to a new date. Counts as a postpone when it moves later.
    pub fn reschedule(&self, id: &str, date: Option<NaiveDate>, time: Option<String>) -> DbResult<()> {
        self.with(|c| {
            c.execute(
                "UPDATE tasks SET
                    postpone_count = postpone_count + CASE
                        WHEN due_date IS NOT NULL AND ?2 IS NOT NULL AND ?2 > due_date THEN 1 ELSE 0 END,
                    due_date = ?2, due_time = ?3,
                    status = CASE WHEN status = 'backlog' THEN 'todo' ELSE status END,
                    backlogged_at = CASE WHEN status = 'backlog' THEN NULL ELSE backlogged_at END
                 WHERE id = ?1",
                params![id, date.map(|d| d.to_string()), time],
            )?;
            Ok(())
        })
    }

    pub fn delete_task(&self, id: &str) -> DbResult<()> {
        self.with(|c| {
            c.execute("DELETE FROM tasks WHERE id = ?1", [id])?;
            Ok(())
        })
    }

    /// Adds `task_id` ← `blocked_by`. Rejects edges that would close a cycle.
    pub fn add_dependency(&self, task_id: &str, blocked_by: &str) -> DbResult<()> {
        self.with(|c| {
            let tx = c.transaction()?;
            add_dependency_tx(&tx, task_id, blocked_by)?;
            tx.commit()?;
            Ok(())
        })
    }

    pub fn remove_dependency(&self, task_id: &str, blocked_by: &str) -> DbResult<()> {
        self.with(|c| {
            c.execute(
                "DELETE FROM task_dependencies WHERE task_id = ?1 AND blocked_by = ?2",
                [task_id, blocked_by],
            )?;
            Ok(())
        })
    }

    /// Todo-bankruptcy guard: open tasks overdue by 3+ days, or postponed 3+
    /// times, move to `backlog`. Returns how many moved.
    pub fn sweep_backlog(&self, today: NaiveDate) -> DbResult<usize> {
        let cutoff = (today - chrono::Duration::days(3)).to_string();
        self.with(|c| {
            Ok(c.execute(
                "UPDATE tasks SET status = 'backlog', backlogged_at = ?2
                 WHERE status = 'todo'
                   AND ((due_date IS NOT NULL AND due_date <= ?1) OR postpone_count >= 3)",
                params![cutoff, now_iso()],
            )?)
        })
    }

    // ── Cycles ───────────────────────────────────────────────────────────

    pub fn create_cycle_pattern(
        &self,
        name: &str,
        anchor: NaiveDate,
        slots: &[CycleSlotInput],
    ) -> DbResult<i64> {
        if slots.is_empty() {
            return Err(DbError::Invalid("주기에 칸이 최소 1개 필요해요".into()));
        }
        self.with(|c| {
            let tx = c.transaction()?;
            tx.execute(
                "INSERT INTO cycle_patterns (name, anchor_date, length) VALUES (?1, ?2, ?3)",
                params![name.trim(), anchor.to_string(), slots.len() as i64],
            )?;
            let id = tx.last_insert_rowid();
            for (i, s) in slots.iter().enumerate() {
                tx.execute(
                    "INSERT INTO cycle_slots (pattern_id, slot_index, label, color) VALUES (?1, ?2, ?3, ?4)",
                    params![id, i as i64, s.label.trim(), s.color],
                )?;
            }
            tx.commit()?;
            Ok(id)
        })
    }

    pub fn list_cycle_patterns(&self) -> DbResult<Vec<CyclePatternRow>> {
        self.with(|c| {
            let mut stmt = c.prepare(
                "SELECT id, name, anchor_date, is_active FROM cycle_patterns ORDER BY id",
            )?;
            let heads = stmt
                .query_map([], |r| {
                    Ok((
                        r.get::<_, i64>(0)?,
                        r.get::<_, String>(1)?,
                        r.get::<_, NaiveDate>(2)?,
                        r.get::<_, bool>(3)?,
                    ))
                })?
                .collect::<Result<Vec<_>, _>>()?;
            let mut out = Vec::with_capacity(heads.len());
            for (id, name, anchor_date, is_active) in heads {
                out.push(CyclePatternRow { id, name, anchor_date, slots: load_slots(c, id)?, is_active });
            }
            Ok(out)
        })
    }

    pub fn set_cycle_override(&self, pattern_id: i64, date: NaiveDate, label: Option<&str>) -> DbResult<()> {
        self.with(|c| {
            match label {
                Some(l) => c.execute(
                    "INSERT INTO cycle_overrides (pattern_id, date, label) VALUES (?1, ?2, ?3)
                     ON CONFLICT(pattern_id, date) DO UPDATE SET label = excluded.label",
                    params![pattern_id, date.to_string(), l],
                )?,
                None => c.execute(
                    "DELETE FROM cycle_overrides WHERE pattern_id = ?1 AND date = ?2",
                    params![pattern_id, date.to_string()],
                )?,
            };
            Ok(())
        })
    }

    /// Cycle state for every day in `[from, to]` — O(1) per day.
    pub fn cycle_days(&self, pattern_id: i64, from: NaiveDate, to: NaiveDate) -> DbResult<Vec<CycleDay>> {
        let pattern = self.load_pattern(pattern_id)?;
        let mut out = Vec::new();
        let mut d = from;
        while d <= to {
            let (label, overridden) = pattern.label_on(d);
            let color = pattern.color_of(label);
            out.push(CycleDay { date: d, label: label.to_string(), color, overridden });
            d = d.succ_opt().expect("date overflow");
        }
        Ok(out)
    }

    /// Active patterns' label on `date`, keyed by pattern id.
    pub fn cycle_labels_on(&self, date: NaiveDate) -> DbResult<HashMap<i64, String>> {
        let ids: Vec<i64> = self.with(|c| {
            let mut stmt = c.prepare("SELECT id FROM cycle_patterns WHERE is_active = 1")?;
            let ids = stmt.query_map([], |r| r.get(0))?.collect::<Result<Vec<_>, _>>()?;
            Ok(ids)
        })?;
        let mut out = HashMap::new();
        for id in ids {
            let p = self.load_pattern(id)?;
            out.insert(id, p.label_on(date).0.to_string());
        }
        Ok(out)
    }

    fn load_pattern(&self, id: i64) -> DbResult<CyclePattern> {
        self.with(|c| {
            let anchor: NaiveDate = c
                .query_row("SELECT anchor_date FROM cycle_patterns WHERE id = ?1", [id], |r| r.get(0))
                .optional()?
                .ok_or_else(|| DbError::Invalid(format!("cycle pattern {id} not found")))?;
            let slots = load_slots(c, id)?;
            let mut stmt = c.prepare("SELECT date, label FROM cycle_overrides WHERE pattern_id = ?1")?;
            let overrides = stmt
                .query_map([id], |r| Ok((r.get::<_, NaiveDate>(0)?, r.get::<_, String>(1)?)))?
                .collect::<Result<HashMap<_, _>, _>>()?;
            Ok(CyclePattern::new(
                anchor,
                slots.into_iter().map(|s| (s.label, s.color)).collect(),
                overrides,
            ))
        })
    }

    // ── Pomodoro & focus tracking ────────────────────────────────────────

    pub fn start_session(&self, task_id: Option<&str>, planned_minutes: u32) -> DbResult<i64> {
        self.with(|c| {
            // At most one running session.
            c.execute(
                "UPDATE pomodoro_sessions SET status = 'aborted', ended_at = ?1 WHERE status = 'running'",
                [now_iso()],
            )?;
            c.execute(
                "INSERT INTO pomodoro_sessions (task_id, planned_minutes, started_at) VALUES (?1, ?2, ?3)",
                params![task_id, planned_minutes, now_iso()],
            )?;
            Ok(c.last_insert_rowid())
        })
    }

    /// Ends a session; focus_score = share of samples spent in focus apps.
    pub fn finish_session(&self, id: i64, completed: bool) -> DbResult<Option<f64>> {
        self.with(|c| {
            let score: Option<f64> = c.query_row(
                "SELECT CASE WHEN COUNT(*) = 0 THEN NULL
                        ELSE CAST(SUM(category = 'focus') AS REAL) / COUNT(*) END
                 FROM focus_samples WHERE session_id = ?1",
                [id],
                |r| r.get(0),
            )?;
            c.execute(
                "UPDATE pomodoro_sessions SET status = ?2, ended_at = ?3, focus_score = ?4 WHERE id = ?1",
                params![id, if completed { "completed" } else { "aborted" }, now_iso(), score],
            )?;
            Ok(score)
        })
    }

    pub fn add_focus_sample(
        &self,
        session_id: i64,
        process_name: &str,
        window_title: &str,
        category: FocusCategory,
    ) -> DbResult<()> {
        self.with(|c| {
            c.execute(
                "INSERT INTO focus_samples (session_id, sampled_at, process_name, window_title, category)
                 VALUES (?1, ?2, ?3, ?4, ?5)",
                params![session_id, now_iso(), process_name, window_title, category.as_str()],
            )?;
            Ok(())
        })
    }
}

fn migrate(conn: &Connection) -> DbResult<()> {
    let current: i64 = conn.pragma_query_value(None, "user_version", |r| r.get(0))?;
    for (i, sql) in MIGRATIONS.iter().enumerate().skip(current as usize) {
        conn.execute_batch(&format!("BEGIN; {sql}\nPRAGMA user_version = {}; COMMIT;", i + 1))?;
    }
    Ok(())
}

const TASK_SELECT: &str = "SELECT t.id, t.title, t.notes, t.status, t.priority, t.due_date, t.due_time,
    t.energy_level, t.estimated_minutes, t.cycle_pattern_id, t.cycle_label, t.postpone_count,
    t.created_at, t.completed_at, COALESCE(b.open_blockers, 0)
    FROM tasks t LEFT JOIN v_task_open_blockers b ON b.task_id = t.id";

fn task_from_row(r: &Row<'_>) -> rusqlite::Result<Task> {
    Ok(Task {
        id: r.get(0)?,
        title: r.get(1)?,
        notes: r.get(2)?,
        status: TaskStatus::parse(&r.get::<_, String>(3)?),
        priority: r.get(4)?,
        due_date: r.get(5)?,
        due_time: r.get(6)?,
        energy_level: r.get::<_, Option<String>>(7)?.as_deref().and_then(EnergyLevel::parse),
        estimated_minutes: r.get(8)?,
        cycle_pattern_id: r.get(9)?,
        cycle_label: r.get(10)?,
        postpone_count: r.get(11)?,
        created_at: r.get(12)?,
        completed_at: r.get(13)?,
        is_blocked: r.get::<_, i64>(14)? > 0,
        tags: Vec::new(),
        blocked_by: Vec::new(),
    })
}

/// Fills tags + blocker ids for a batch of tasks (two queries, not N+1).
fn hydrate(c: &Connection, mut tasks: Vec<Task>) -> DbResult<Vec<Task>> {
    if tasks.is_empty() {
        return Ok(tasks);
    }
    let ids: HashSet<&str> = tasks.iter().map(|t| t.id.as_str()).collect();
    let mut tags: HashMap<String, Vec<String>> = HashMap::new();
    let mut stmt = c.prepare(
        "SELECT tt.task_id, g.name FROM task_tags tt JOIN tags g ON g.id = tt.tag_id ORDER BY g.name",
    )?;
    for row in stmt.query_map([], |r| Ok((r.get::<_, String>(0)?, r.get::<_, String>(1)?)))? {
        let (tid, name) = row?;
        if ids.contains(tid.as_str()) {
            tags.entry(tid).or_default().push(name);
        }
    }
    let mut deps: HashMap<String, Vec<String>> = HashMap::new();
    let mut stmt = c.prepare("SELECT task_id, blocked_by FROM task_dependencies")?;
    for row in stmt.query_map([], |r| Ok((r.get::<_, String>(0)?, r.get::<_, String>(1)?)))? {
        let (tid, by) = row?;
        if ids.contains(tid.as_str()) {
            deps.entry(tid).or_default().push(by);
        }
    }
    for t in &mut tasks {
        t.tags = tags.remove(&t.id).unwrap_or_default();
        t.blocked_by = deps.remove(&t.id).unwrap_or_default();
    }
    Ok(tasks)
}

fn attach_tag(c: &Connection, task_id: &str, name: &str) -> DbResult<()> {
    let name = name.trim().trim_start_matches('#');
    if name.is_empty() {
        return Ok(());
    }
    c.execute("INSERT INTO tags (name) VALUES (?1) ON CONFLICT(name) DO NOTHING", [name])?;
    c.execute(
        "INSERT OR IGNORE INTO task_tags (task_id, tag_id) SELECT ?1, id FROM tags WHERE name = ?2",
        [task_id, name],
    )?;
    Ok(())
}

/// True when `from` can reach `target` by following blocked_by edges.
fn reaches(c: &Connection, from: &str, target: &str) -> DbResult<bool> {
    let found: Option<i64> = c
        .query_row(
            "WITH RECURSIVE up(id) AS (
                SELECT ?1
                UNION
                SELECT d.blocked_by FROM task_dependencies d JOIN up ON d.task_id = up.id
             )
             SELECT 1 FROM up WHERE id = ?2 LIMIT 1",
            [from, target],
            |r| r.get(0),
        )
        .optional()?;
    Ok(found.is_some())
}

fn add_dependency_tx(c: &Connection, task_id: &str, blocked_by: &str) -> DbResult<()> {
    if task_id == blocked_by {
        return Err(DbError::Invalid("작업이 자기 자신을 기다릴 수는 없어요".into()));
    }
    // New edge task → blocked_by closes a cycle iff blocked_by already
    // (transitively) waits on task_id.
    if reaches(c, blocked_by, task_id)? {
        return Err(DbError::Invalid("순환 의존성이 생겨서 추가할 수 없어요".into()));
    }
    c.execute(
        "INSERT OR IGNORE INTO task_dependencies (task_id, blocked_by) VALUES (?1, ?2)",
        [task_id, blocked_by],
    )?;
    Ok(())
}

fn load_slots(c: &Connection, pattern_id: i64) -> DbResult<Vec<CycleSlotInput>> {
    let mut stmt = c.prepare(
        "SELECT label, color FROM cycle_slots WHERE pattern_id = ?1 ORDER BY slot_index",
    )?;
    let slots = stmt
        .query_map([pattern_id], |r| Ok(CycleSlotInput { label: r.get(0)?, color: r.get(1)? }))?
        .collect::<Result<Vec<_>, _>>()?;
    Ok(slots)
}

pub fn today_local() -> NaiveDate {
    Local::now().date_naive()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn d(s: &str) -> NaiveDate {
        NaiveDate::parse_from_str(s, "%Y-%m-%d").unwrap()
    }

    fn task(db: &Db, title: &str, due: Option<&str>) -> Task {
        db.insert_task(NewTask { title: title.into(), due_date: due.map(d), ..Default::default() })
            .unwrap()
    }

    #[test]
    fn migrations_are_idempotent_and_wal() {
        let dir = std::env::temp_dir().join(format!("bt-{}", uuid::Uuid::new_v4()));
        let path = dir.join("t.db");
        drop(Db::open(&path).unwrap());
        let db = Db::open(&path).unwrap();
        let mode: String = db.with(|c| Ok(c.pragma_query_value(None, "journal_mode", |r| r.get(0))?)).unwrap();
        assert_eq!(mode.to_lowercase(), "wal");
        let v: i64 = db.with(|c| Ok(c.pragma_query_value(None, "user_version", |r| r.get(0))?)).unwrap();
        assert_eq!(v, MIGRATIONS.len() as i64);
        let _ = std::fs::remove_dir_all(dir);
    }

    #[test]
    fn dependencies_block_and_reject_cycles() {
        let db = Db::open_in_memory().unwrap();
        let a = task(&db, "A", None);
        let b = db
            .insert_task(NewTask { title: "B".into(), blocked_by: vec![a.id.clone()], ..Default::default() })
            .unwrap();
        assert!(b.is_blocked);
        let c = task(&db, "C", None);
        db.add_dependency(&c.id, &b.id).unwrap(); // C ← B ← A
        assert!(db.add_dependency(&a.id, &c.id).is_err(), "A ← C would close a cycle");
        assert!(db.add_dependency(&a.id, &a.id).is_err());
        db.set_status(&a.id, TaskStatus::Done).unwrap();
        assert!(!db.get_task(&b.id).unwrap().unwrap().is_blocked);
    }

    #[test]
    fn backlog_sweep_quarantines_stale_tasks() {
        let db = Db::open_in_memory().unwrap();
        let today = d("2026-11-11");
        let stale = task(&db, "stale", Some("2026-11-08"));
        let fresh = task(&db, "fresh", Some("2026-11-09"));
        let bumped = task(&db, "bumped", Some("2026-11-11"));
        for day in ["2026-11-12", "2026-11-13", "2026-11-14"] {
            db.reschedule(&bumped.id, Some(d(day)), None).unwrap();
        }
        assert_eq!(db.sweep_backlog(today).unwrap(), 2);
        assert_eq!(db.get_task(&stale.id).unwrap().unwrap().status, TaskStatus::Backlog);
        assert_eq!(db.get_task(&fresh.id).unwrap().unwrap().status, TaskStatus::Todo);
        assert_eq!(db.get_task(&bumped.id).unwrap().unwrap().status, TaskStatus::Backlog);
    }

    #[test]
    fn cycle_bound_tasks_show_only_on_matching_day() {
        let db = Db::open_in_memory().unwrap();
        let slots: Vec<CycleSlotInput> = ["주간", "야간", "비번", "휴무"]
            .iter()
            .map(|l| CycleSlotInput { label: l.to_string(), color: None })
            .collect();
        let pid = db.create_cycle_pattern("교대", d("2026-11-01"), &slots).unwrap();
        db.insert_task(NewTask {
            title: "야간 전 낮잠".into(),
            cycle_pattern_id: Some(pid),
            cycle_label: Some("야간".into()),
            ..Default::default()
        })
        .unwrap();
        // 11-02 is slot 1 = 야간, 11-03 is 비번.
        assert_eq!(db.list_tasks(TaskView::Today, d("2026-11-02")).unwrap().len(), 1);
        assert_eq!(db.list_tasks(TaskView::Today, d("2026-11-03")).unwrap().len(), 0);
        db.set_cycle_override(pid, d("2026-11-03"), Some("야간")).unwrap();
        assert_eq!(db.list_tasks(TaskView::Today, d("2026-11-03")).unwrap().len(), 1);
        let days = db.cycle_days(pid, d("2026-10-31"), d("2026-11-04")).unwrap();
        let labels: Vec<_> = days.iter().map(|x| x.label.as_str()).collect();
        assert_eq!(labels, ["휴무", "주간", "야간", "야간", "휴무"]);
    }

    #[test]
    fn focus_score_is_share_of_focus_samples() {
        let db = Db::open_in_memory().unwrap();
        let s = db.start_session(None, 25).unwrap();
        db.add_focus_sample(s, "code", "lib.rs", FocusCategory::Focus).unwrap();
        db.add_focus_sample(s, "code", "db.rs", FocusCategory::Focus).unwrap();
        db.add_focus_sample(s, "chrome", "YouTube", FocusCategory::Distraction).unwrap();
        db.add_focus_sample(s, "explorer", "", FocusCategory::Neutral).unwrap();
        assert_eq!(db.finish_session(s, true).unwrap(), Some(0.5));
    }
}
