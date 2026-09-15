use tauri::{Emitter, Manager};
use tauri_plugin_deep_link::DeepLinkExt;


#[tauri::command]
fn save_download_file(filename: String, content: String) -> Result<String, String> {
    let base_dir = std::env::var("USERPROFILE")
        .map(|p| std::path::PathBuf::from(p).join("Downloads"))
        .or_else(|_| std::env::var("HOME").map(|p| std::path::PathBuf::from(p).join("Downloads")))
        .unwrap_or_else(|_| std::env::current_dir().unwrap_or_default());

    if !base_dir.exists() {
        let _ = std::fs::create_dir_all(&base_dir);
    }

    let mut file_path = base_dir.join(&filename);
    if file_path.exists() {
        let stem = std::path::Path::new(&filename)
            .file_stem()
            .and_then(|s| s.to_str())
            .unwrap_or("file");
        let ext = std::path::Path::new(&filename)
            .extension()
            .and_then(|s| s.to_str())
            .unwrap_or("json");

        let mut counter = 1;
        while file_path.exists() && counter < 1000 {
            file_path = base_dir.join(format!("{}_{}.{}", stem, counter, ext));
            counter += 1;
        }
    }

    std::fs::write(&file_path, content)
        .map_err(|e| format!("Failed to write file: {}", e))?;

    Ok(file_path.to_string_lossy().to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    std::panic::set_hook(Box::new(|info| {
        let msg = format!("[CRITICAL PANIC] {:?}", info);
        eprintln!("{}", msg);
        let _ = std::fs::write("panic.log", msg);
    }));

    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, args, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.unminimize();
                let _ = window.show();
                let _ = window.set_focus();
            }
            for arg in &args {
                if arg.starts_with("crescendo://") {
                    let _ = app.emit("deep-link://new-url", vec![arg.clone()]);
                }
            }
        }))
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_deep_link::init())
        .invoke_handler(tauri::generate_handler![save_download_file])
        .setup(|app| {
            #[cfg(desktop)]
            {
                let _ = app.deep_link().register("crescendo");
            }

            // In release builds, serve bundled local assets (../dist) by default.
            // If the user explicitly sets CRESCENDO_REMOTE_URL, navigate to that remote endpoint.
            #[cfg(not(debug_assertions))]
            {
                if let Ok(target_host) = std::env::var("CRESCENDO_REMOTE_URL") {
                    if !target_host.is_empty() && target_host != "local" {
                        let handle = app.handle().clone();
                        std::thread::spawn(move || {
                            if let Some(window) = handle.get_webview_window("main") {
                                if let Ok(url) = target_host.parse() {
                                    let _ = window.navigate(url);
                                }
                            }
                        });
                    }
                }
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running crescendo desktop application");
}

