use tauri::{Manager, menu::{Menu, MenuItem}, tray::{TrayIconBuilder, TrayIconEvent}};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_autostart::Builder::new().build())
        .plugin(tauri_plugin_global_shortcut::Builder::new().build())
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                api.prevent_close();
                let _ = window.hide();
            }
        })
        .setup(|app| {
            let show = MenuItem::with_id(app, "show", "BetterTick 열기", true, None::<&str>)?;
            let quit = MenuItem::with_id(app, "quit", "종료", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&show, &quit])?;
            let mut tray_builder = TrayIconBuilder::new();
            if let Some(icon) = app.default_window_icon() { tray_builder = tray_builder.icon(icon.clone()); }
            tray_builder.menu(&menu)
                .on_menu_event(|app, event| match event.id().as_ref() {
                    "show" => { if let Some(window) = app.get_webview_window("main") { let _ = window.show(); let _ = window.set_focus(); } },
                    "quit" => app.exit(0),
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| { if let TrayIconEvent::DoubleClick { .. } = event { if let Some(window) = tray.app_handle().get_webview_window("main") { let _ = window.show(); let _ = window.set_focus(); } } })
                .build(app)?;
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running BetterTick");
}
