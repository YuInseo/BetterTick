//! IPC commands invoked from the React frontend (`invoke("…")`).
//! Every mutation emits `tasks:changed` so all windows refresh.

use chrono::NaiveDate;
use tauri::{AppHandle, Emitter, Manager, State};

use crate::db::{
    today_local, CycleDay, CyclePatternRow, CycleSlotInput, Db, NewTask, Task, TaskStatus, TaskView,
};
use crate::parser::{self, ParsedQuickAdd};
use crate::pomodoro::{self, PomodoroState, PomodoroTick};

type CmdResult<T> = Result<T, String>;

fn changed(app: &AppHandle) {
    let _ = app.emit("tasks:changed", ());
}

fn err(e: impl ToString) -> String {
    e.to_string()
}

// ── Tasks ────────────────────────────────────────────────────────────────

#[tauri::command]
pub fn list_tasks(db: State<'_, Db>, view: TaskView) -> CmdResult<Vec<Task>> {
    db.list_tasks(view, today_local()).map_err(err)
}

#[tauri::command]
pub fn create_task(app: AppHandle, db: State<'_, Db>, task: NewTask) -> CmdResult<Task> {
    let t = db.insert_task(task).map_err(err)?;
    changed(&app);
    Ok(t)
}

/// Live preview for the quick-add bar.
#[tauri::command]
pub fn parse_quick_add(text: String) -> ParsedQuickAdd {
    parser::parse(&text, today_local())
}

/// Parses and saves a quick-add line, then hides the quick-add window.
#[tauri::command]
pub fn quick_add(app: AppHandle, db: State<'_, Db>, text: String) -> CmdResult<Task> {
    let parsed = parser::parse(&text, today_local());
    if parsed.title.trim().is_empty() {
        return Err("할 일 제목을 입력해 주세요".into());
    }
    let t = db.insert_task(parsed.into_new_task()).map_err(err)?;
    changed(&app);
    if let Some(w) = app.get_webview_window("quickadd") {
        let _ = w.hide();
    }
    Ok(t)
}

#[tauri::command]
pub fn set_task_status(app: AppHandle, db: State<'_, Db>, id: String, status: TaskStatus) -> CmdResult<()> {
    db.set_status(&id, status).map_err(err)?;
    changed(&app);
    Ok(())
}

#[tauri::command]
pub fn reschedule_task(
    app: AppHandle,
    db: State<'_, Db>,
    id: String,
    date: Option<NaiveDate>,
    time: Option<String>,
) -> CmdResult<()> {
    db.reschedule(&id, date, time).map_err(err)?;
    changed(&app);
    Ok(())
}

#[tauri::command]
pub fn delete_task(app: AppHandle, db: State<'_, Db>, id: String) -> CmdResult<()> {
    db.delete_task(&id).map_err(err)?;
    changed(&app);
    Ok(())
}

#[tauri::command]
pub fn add_dependency(app: AppHandle, db: State<'_, Db>, task_id: String, blocked_by: String) -> CmdResult<()> {
    db.add_dependency(&task_id, &blocked_by).map_err(err)?;
    changed(&app);
    Ok(())
}

#[tauri::command]
pub fn remove_dependency(app: AppHandle, db: State<'_, Db>, task_id: String, blocked_by: String) -> CmdResult<()> {
    db.remove_dependency(&task_id, &blocked_by).map_err(err)?;
    changed(&app);
    Ok(())
}

#[tauri::command]
pub fn run_backlog_sweep(app: AppHandle, db: State<'_, Db>) -> CmdResult<usize> {
    let n = db.sweep_backlog(today_local()).map_err(err)?;
    if n > 0 {
        changed(&app);
    }
    Ok(n)
}

// ── Cycles ───────────────────────────────────────────────────────────────

#[tauri::command]
pub fn create_cycle_pattern(
    app: AppHandle,
    db: State<'_, Db>,
    name: String,
    anchor_date: NaiveDate,
    slots: Vec<CycleSlotInput>,
) -> CmdResult<i64> {
    let id = db.create_cycle_pattern(&name, anchor_date, &slots).map_err(err)?;
    changed(&app);
    Ok(id)
}

#[tauri::command]
pub fn list_cycle_patterns(db: State<'_, Db>) -> CmdResult<Vec<CyclePatternRow>> {
    db.list_cycle_patterns().map_err(err)
}

#[tauri::command]
pub fn cycle_days(db: State<'_, Db>, pattern_id: i64, from: NaiveDate, to: NaiveDate) -> CmdResult<Vec<CycleDay>> {
    if (to - from).num_days() > 400 {
        return Err("range too large".into());
    }
    db.cycle_days(pattern_id, from, to).map_err(err)
}

#[tauri::command]
pub fn set_cycle_override(
    app: AppHandle,
    db: State<'_, Db>,
    pattern_id: i64,
    date: NaiveDate,
    label: Option<String>,
) -> CmdResult<()> {
    db.set_cycle_override(pattern_id, date, label.as_deref()).map_err(err)?;
    changed(&app);
    Ok(())
}

// ── Pomodoro / windows ───────────────────────────────────────────────────

#[tauri::command]
pub fn pomodoro_start(app: AppHandle, task_id: Option<String>, minutes: u32) -> CmdResult<PomodoroTick> {
    pomodoro::start(&app, task_id, minutes)
}

#[tauri::command]
pub fn pomodoro_stop(app: AppHandle) -> CmdResult<()> {
    pomodoro::stop(&app, false)
}

#[tauri::command]
pub fn pomodoro_status(state: State<'_, PomodoroState>) -> PomodoroTick {
    state.snapshot()
}

#[tauri::command]
pub fn set_pip_interactive(app: AppHandle, interactive: bool) {
    pomodoro::set_pip_interactive(&app, interactive);
}

#[tauri::command]
pub fn hide_quick_add(app: AppHandle) {
    if let Some(w) = app.get_webview_window("quickadd") {
        let _ = w.hide();
    }
}

#[tauri::command]
pub fn toggle_quick_add(app: AppHandle) {
    crate::toggle_quick_add(&app);
}

#[tauri::command]
pub fn show_main(app: AppHandle) {
    crate::show_main_window(&app);
}
