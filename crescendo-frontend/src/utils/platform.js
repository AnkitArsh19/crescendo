/**
 * Desktop Platform & Runtime Detection Helpers
 */

export const APP_VERSION = '1.0.1';

export const isTauri = () => {
  return typeof window !== 'undefined' && Boolean(window.__TAURI_INTERNALS__);
};

export const getPlatform = () => {
  if (typeof navigator === 'undefined') return 'web';
  const ua = navigator.userAgent.toLowerCase();
  if (ua.includes('macintosh') || ua.includes('mac os')) return 'macos';
  if (ua.includes('windows')) return 'windows';
  if (ua.includes('linux')) return 'linux';
  return 'web';
};

export const isMac = () => getPlatform() === 'macos';
export const isWindows = () => getPlatform() === 'windows';
export const isLinux = () => getPlatform() === 'linux';
