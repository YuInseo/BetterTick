//! Pomodoro timer + PIP widget + active-window focus tracking.
//!
//! The timer lives in Rust so every window (main, quick-add, PIP) sees the same
//! state through `pomodoro:tick` events. While a session runs, the focused OS
//! window is sampled every few seconds and classified into
//! focus / neutral / distraction; the session's focus_score is the share of
//! focus samples.

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use std::time::{Duration, Instant};

use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager, PhysicalPosition, Runtime};

use crate::db::{Db, FocusCategory};

const SAMPLE_EVERY: Duration = Duration::from_secs(5);
const PIP_MARGIN: i32 = 16;

#[derive(Clone, Serialize)]
pub struct PomodoroTick {
    pub running: bool,
    pub session_id: Option<i64>,
    pub task_id: Option<String>,
    pub task_title: Option<String>,
    pub total_secs: u64,
    pub remaining_secs: u64,
    /// Last sampled foreground app ("Code", "chrome", …).
    pub current_app: Option<String>,
    pub current_category: Option<FocusCategory>,
}

impl PomodoroTick {
    fn idle() -> Self {
        Self {
            running: false,
            session_id: None,
            task_id: None,
            task_title: None,
            total_secs: 0,
            remaining_secs: 0,
            current_app: None,
            current_category: None,
        }
    }
}

struct Running {
    session_id: i64,
    task_id: Option<String>,
    task_title: Option<String>,
    total: Duration,
    started: Instant,
    last_app: Option<(String, FocusCategory)>,
}

#[derive(Default)]
pub struct PomodoroState {
    running: Mutex<Option<Running>>,
    /// PIP accepts clicks only while true (toggled by Ctrl+Alt+P / tray).
    pip_interactive: AtomicBool,
}

impl PomodoroState {
    pub fn snapshot(&self) -> PomodoroTick {
        let guard = self.running.lock().unwrap();
        match guard.as_ref() {
            None => PomodoroTick::idle(),
            Some(r) => PomodoroTick {
                running: true,
                session_id: Some(r.session_id),
                task_id: r.task_id.clone(),
                task_title: r.task_title.clone(),
                total_secs: r.total.as_secs(),
                remaining_secs: r.total.saturating_sub(r.started.elapsed()).as_secs(),
                current_app: r.last_app.as_ref().map(|(a, _)| a.clone()),
                current_category: r.last_app.as_ref().map(|(_, c)| *c),
            },
        }
    }

    pub fn is_running(&self) -> bool {
        self.running.lock().unwrap().is_some()
    }

    pub fn pip_interactive(&self) -> bool {
        self.pip_interactive.load(Ordering::Relaxed)
    }
}

pub fn start<R: Runtime>(
    app: &AppHandle<R>,
    task_id: Option<String>,
    minutes: u32,
) -> Result<PomodoroTick, String> {
    let minutes = minutes.clamp(1, 240);
    let db = app.state::<Db>();
    let task_title = match &task_id {
        Some(id) => db.get_task(id).map_err(|e| e.to_string())?.map(|t| t.title),
        None => None,
    };
    let session_id = db.start_session(task_id.as_deref(), minutes).map_err(|e| e.to_string())?;
    {
        let state = app.state::<PomodoroState>();
        *state.running.lock().unwrap() = Some(Running {
            session_id,
            task_id,
            task_title,
            total: Duration::from_secs(minutes as u64 * 60),
            started: Instant::now(),
            last_app: None,
        });
    }
    show_pip(app);
    spawn_ticker(app.clone(), session_id);
    let snap = app.state::<PomodoroState>().snapshot();
    let _ = app.emit("pomodoro:tick", &snap);
    Ok(snap)
}

/// Stops the current session (aborted unless it ran to the end).
pub fn stop<R: Runtime>(app: &AppHandle<R>, completed: bool) -> Result<(), String> {
    let taken = app.state::<PomodoroState>().running.lock().unwrap().take();
    if let Some(r) = taken {
        let score = app
            .state::<Db>()
            .finish_session(r.session_id, completed)
            .map_err(|e| e.to_string())?;
        let _ = app.emit(
            "pomodoro:finished",
            serde_json::json!({ "session_id": r.session_id, "completed": completed, "focus_score": score }),
        );
    }
    if let Some(pip) = app.get_webview_window("pip") {
        let _ = pip.hide();
    }
    let _ = app.emit("pomodoro:tick", PomodoroTick::idle());
    Ok(())
}

pub fn set_pip_interactive<R: Runtime>(app: &AppHandle<R>, interactive: bool) {
    app.state::<PomodoroState>().pip_interactive.store(interactive, Ordering::Relaxed);
    if let Some(pip) = app.get_webview_window("pip") {
        let _ = pip.set_ignore_cursor_events(!interactive);
        if interactive {
            let _ = pip.set_focus();
        }
    }
    let _ = app.emit("pip:interactive", interactive);
}

pub fn toggle_pip_interactive<R: Runtime>(app: &AppHandle<R>) {
    let now = app.state::<PomodoroState>().pip_interactive();
    set_pip_interactive(app, !now);
}

fn show_pip<R: Runtime>(app: &AppHandle<R>) {
    let Some(pip) = app.get_webview_window("pip") else { return };
    // Pin to the top-right corner of the primary monitor.
    if let (Ok(Some(mon)), Ok(size)) = (pip.primary_monitor(), pip.outer_size()) {
        let m = (PIP_MARGIN as f64 * mon.scale_factor()) as i32;
        let x = mon.position().x + mon.size().width as i32 - size.width as i32 - m;
        let y = mon.position().y + m;
        let _ = pip.set_position(PhysicalPosition::new(x, y));
    }
    let _ = pip.set_always_on_top(true);
    let _ = pip.show();
    // Click-through by default so it never steals clicks.
    let interactive = app.state::<PomodoroState>().pip_interactive();
    let _ = pip.set_ignore_cursor_events(!interactive);
}

fn spawn_ticker<R: Runtime>(app: AppHandle<R>, session_id: i64) {
    tauri::async_runtime::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_secs(1));
        let mut last_sample = Instant::now() - SAMPLE_EVERY;
        loop {
            interval.tick().await;
            let state = app.state::<PomodoroState>();
            // A newer session (or stop) replaced ours → exit.
            let (done, still_ours) = {
                let guard = state.running.lock().unwrap();
                match guard.as_ref() {
                    Some(r) if r.session_id == session_id => (r.started.elapsed() >= r.total, true),
                    _ => (false, false),
                }
            };
            if !still_ours {
                break;
            }
            if done {
                let _ = stop(&app, true);
                break;
            }
            if last_sample.elapsed() >= SAMPLE_EVERY {
                last_sample = Instant::now();
                if let Some((process, title)) = active_window() {
                    let category = classify(&process, &title);
                    let _ = app.state::<Db>().add_focus_sample(session_id, &process, &title, category);
                    if let Some(r) = state.running.lock().unwrap().as_mut() {
                        r.last_app = Some((process, category));
                    }
                }
            }
            let _ = app.emit("pomodoro:tick", state.snapshot());
        }
    });
}

/// (process name, window title) of the OS foreground window.
fn active_window() -> Option<(String, String)> {
    let w = active_win_pos_rs::get_active_window().ok()?;
    let process = if w.app_name.is_empty() {
        w.process_path.file_stem()?.to_string_lossy().into_owned()
    } else {
        w.app_name
    };
    Some((process, w.title))
}

/// Heuristic split of the foreground window into focus / neutral / distraction.
pub fn classify(process: &str, title: &str) -> FocusCategory {
    let p = process.to_lowercase();
    let t = title.to_lowercase();
    const FOCUS_APPS: &[&str] = &[
        "code", "cursor", "zed", "devenv", "idea", "pycharm", "webstorm", "clion", "rider", "goland",
        "rustrover", "android studio", "studio64", "sublime", "nvim", "vim", "emacs", "windowsterminal",
        "terminal", "powershell", "pwsh", "cmd", "alacritty", "wezterm", "kitty", "iterm", "obsidian",
        "notion", "figma", "postman", "datagrip", "dbeaver", "bettertick", "word", "excel", "hwp",
    ];
    const DISTRACTION_APPS: &[&str] = &[
        "discord", "kakaotalk", "telegram", "steam", "league", "valorant", "battle.net", "netflix",
    ];
    const DISTRACTION_TITLES: &[&str] = &[
        "youtube", "netflix", "twitch", "instagram", "facebook", "reddit", "tiktok", "치지직", "숲",
        "웹툰", "나무위키", "x.com", "twitter",
    ];
    const FOCUS_TITLES: &[&str] = &[
        "github", "stack overflow", "stackoverflow", "docs", "documentation", "mdn", "localhost",
        "crates.io", "docs.rs", "chatgpt", "claude",
    ];
    if DISTRACTION_APPS.iter().any(|a| p.contains(a)) || DISTRACTION_TITLES.iter().any(|k| t.contains(k)) {
        FocusCategory::Distraction
    } else if FOCUS_APPS.iter().any(|a| p == *a || p.starts_with(a)) || FOCUS_TITLES.iter().any(|k| t.contains(k)) {
        FocusCategory::Focus
    } else {
        FocusCategory::Neutral
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn classifies_common_apps() {
        assert_eq!(classify("Code", "lib.rs — bettertick"), FocusCategory::Focus);
        assert_eq!(classify("chrome", "YouTube - Lo-fi"), FocusCategory::Distraction);
        assert_eq!(classify("chrome", "tauri - GitHub"), FocusCategory::Focus);
        assert_eq!(classify("explorer", "Downloads"), FocusCategory::Neutral);
        assert_eq!(classify("KakaoTalk", "채팅"), FocusCategory::Distraction);
    }
}
