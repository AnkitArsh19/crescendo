import { isTauri } from './platform';
import useToastStore from '../store/toastStore';

/**
 * Universal file downloader for both Desktop (Tauri/WebView2) and Web Browser.
 * In desktop mode: uses the native Rust file saver to save directly into the user's Downloads folder.
 * In browser mode: uses an ObjectURL and invisible <a> download anchor.
 */
export async function downloadFile(filename, content, mimeType = 'application/json') {
  const text = typeof content === 'string' ? content : JSON.stringify(content, null, 2);

  if (isTauri()) {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const savedPath = await invoke('save_download_file', { filename, content: text });
      useToastStore.getState().addToast({
        type: 'success',
        message: `Saved to Downloads: ${filename}`,
      });
      return { success: true, path: savedPath };
    } catch (err) {
      console.warn('Tauri save_download_file failed, falling back to browser download:', err);
    }
  }

  // Web browser download
  try {
    const blob = new Blob([text], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    useToastStore.getState().addToast({
      type: 'success',
      message: `Downloaded: ${filename}`,
    });
    return { success: true };
  } catch (err) {
    console.error('Download failed:', err);
    useToastStore.getState().addToast({
      type: 'error',
      message: `Failed to download ${filename}`,
    });
    return { success: false, error: err };
  }
}
