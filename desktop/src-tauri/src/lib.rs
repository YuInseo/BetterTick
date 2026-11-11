//! BetterTick desktop — Tauri v2 backend entry.
//!
//! - Main window hides to the system tray on close (the app keeps running).
//! - `Alt+Space` toggles the frameless Raycast-style quick-add bar from anywhere.
//! - `Ctrl+Alt+P` toggles whether the PIP pomodoro widget accepts clicks.
//! - Hourly backlog sweep quarantines stale tasks (todo bankruptcy guard).

mod commands;
mod cycle;
pub mod db;
pub mod parser;
mod pomodoro;

use std::time::Duration;

use tauri::menu::{Menu, MenuItem, PredefinedMenuItem};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{AppHandle, Emitter, Manager, Runtime, WindowEvent};

use crate::db::Db;
use crate::pomodoro::PomodoroState;

pub fn show_main_window<R: Runtime>(app: &AppHandle<R>) {
    if let Some(w) = app.get_webview_window("main") {
        let _ = w.unminimize();
        let _ = w.show();
        let _ = w.set_focus();
    }
}

/// Shows the quick-add bar centered and focused, or hides it if it is already up.
pub fn toggle_quick_add<R: Runtime>(app: &AppHandle<R>) {
    let Some(w) = app.get_webview_window("quickadd") else { return };
    if w.is_visible().unwrap_or(false) {
        let _ = w.hide();
        return;
    }
    let _ = w.center();
    let _ = w.show();
    let _ = w.set_always_on_top(true);
    let _ = w.set_focus();
    // Lets the page clear its input and focus it.
    let _ = w.emit("quickadd:opened", ());
}

#[cfg(desktop)]
fn setup_shortcuts(app: &tauri::App) -> tauri::Result<()> {
    use tauri_plugin_global_shortcut::{Code, GlobalShortcutExt, Modifiers, Shortcut, ShortcutState};

    let quick_add = Shortcut::new(Some(Modifiers::ALT), Code::Space);
    // Fallback if another app (PowerToys Run, etc.) already owns Alt+Space.
    let quick_add_alt = Shortcut::new(Some(Modifiers::CONTROL | Modifiers::ALT), Code::Space);
    let pip_toggle = Shortcut::new(Some(Modifiers::CONTROL | Modifiers::ALT), Code::KeyP);

    app.handle().plugin(
        tauri_plugin_global_shortcut::Builder::new()
            .with_handler(move |app, shortcut, event| {
                if event.state() != ShortcutState::Pressed {
                    return;
                }
                if shortcut == &quick_add || shortcut == &quick_add_alt {
                    toggle_quick_add(app);
                } else if shortcut == &pip_toggle {
                    pomodoro::toggle_pip_interactive(app);
                }
            })
            .build(),
    )?;

    let gs = app.global_shortcut();
    if let Err(e) = gs.register(quick_add) {
        log::warn!("Alt+Space unavailable ({e}); using Ctrl+Alt+Space");
        if let Err(e) = gs.register(quick_add_alt) {
            log::error!("quick-add shortcut registration failed: {e}");
        }
    }
    if let Err(e) = gs.register(pip_toggle) {
        log::warn!("Ctrl+Alt+P registration failed: {e}");
    }
    Ok(())
}

fn setup_tray(app: &tauri::App) -> tauri::Result<()> {
    let open = MenuItem::with_id(app, "open", "BetterTick 열기", true, None::<&str>)?;
    let quick = MenuItem::with_id(app, "quick_add", "빠른 추가  (Alt+Space)", true, None::<&str>)?;
    let pomo = MenuItem::with_id(app, "pomodoro", "포모도로 25분 시작 / 정지", true, None::<&str>)?;
    let pip = MenuItem::with_id(app, "pip_toggle", "PIP 클릭 허용 전환  (Ctrl+Alt+P)", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "종료", true, None::<&str>)?;
    let sep = PredefinedMenuItem::separator(app)?;
    let sep2 = PredefinedMenuItem::separator(app)?;
    let menu = Menu::with_items(app, &[&open, &quick, &sep, &pomo, &pip, &sep2, &quit])?;

    let mut tray = TrayIconBuilder::with_id("bettertick-tray")
        .tooltip("BetterTick")
        .menu(&menu)
        .show_menu_on_left_click(false)
        .on_menu_event(|app, event| match event.id.as_ref() {
            "open" => show_main_window(app),
            "quick_add" => toggle_quick_add(app),
            "pomodoro" => {
                if app.state::<PomodoroState>().is_running() {
                    let _ = pomodoro::stop(app, false);
                } else {
                    let _ = pomodoro::start(app, None, 25);
                }
            }
            "pip_toggle" => pomodoro::toggle_pip_interactive(app),
            "quit" => app.exit(0),
            _ => {}
        })
        .on_tray_icon_event(|tray, event| {
            if let TrayIconEvent::Click { button: MouseButton::Left, button_state: MouseButtonState::Up, .. } = event {
                show_main_window(tray.app_handle());
            }
        });
    if let Some(icon) = app.default_window_icon() {
        tray = tray.icon(icon.clone());
    }
    tray.build(app)?;
    Ok(())
}

/// Backlog sweep now and every hour.
fn spawn_backlog_sweeper(app: AppHandle) {
    tauri::async_runtime::spawn(async move {
        let mut every = tokio::time::interval(Duration::from_secs(60 * 60));
        loop {
            every.tick().await;
            match app.state::<Db>().sweep_backlog(db::today_local()) {
                Ok(n) if n > 0 => {
                    log::info!("backlog sweep moved {n} task(s)");
                    let _ = app.emit("tasks:changed", ());
                }
                Ok(_) => {}
                Err(e) => log::error!("backlog sweep failed: {e}"),
            }
        }
    });
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .manage(PomodoroState::default())
        .setup(|app| {
            let dir = app.path().app_data_dir()?;
            let db = Db::open(&dir.join("bettertick.db")).map_err(|e| Box::new(e) as Box<dyn std::error::Error>)?;
            app.manage(db);

            #[cfg(desktop)]
            setup_shortcuts(app)?;
            setup_tray(app)?;
            spawn_backlog_sweeper(app.handle().clone());
            Ok(())
        })
        .on_window_event(|window, event| match (window.label(), event) {
            // Close = hide to tray. Quit lives in the tray menu.
            (_, WindowEvent::CloseRequested { api, .. }) => {
                api.prevent_close();
                let _ = window.hide();
            }
            // Raycast-style: the quick-add bar disappears when it loses focus.
            ("quickadd", WindowEvent::Focused(false)) => {
                let _ = window.hide();
            }
            _ => {}
        })
        .invoke_handler(tauri::generate_handler![
            commands::list_tasks,
            commands::create_task,
            commands::parse_quick_add,
            commands::quick_add,
            commands::set_task_status,
            commands::reschedule_task,
            commands::delete_task,
            commands::add_dependency,
            commands::remove_dependency,
            commands::run_backlog_sweep,
            commands::create_cycle_pattern,
            commands::list_cycle_patterns,
            commands::cycle_days,
            commands::set_cycle_override,
            commands::pomodoro_start,
            commands::pomodoro_stop,
            commands::pomodoro_status,
            commands::set_pip_interactive,
            commands::hide_quick_add,
            commands::toggle_quick_add,
            commands::show_main,
        ])
        .run(tauri::generate_context!())
        .expect("error while running BetterTick");
}
