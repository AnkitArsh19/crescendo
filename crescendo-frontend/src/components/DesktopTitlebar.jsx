import React, { useState, useEffect } from 'react';
import { isTauri, isMac, isWindows } from '../utils/platform';
import { useTheme } from './ThemeContext';
import './DesktopTitlebar.css';

export default function DesktopTitlebar() {
  const [appWindow, setAppWindow] = useState(null);
  const [isMaximized, setIsMaximized] = useState(false);
  const { theme } = useTheme();

  useEffect(() => {
    if (!isTauri()) return;

    let unlisten = null;
    import('@tauri-apps/api/window').then(({ getCurrentWindow }) => {
      const win = getCurrentWindow();
      setAppWindow(win);

      win.isMaximized().then(setIsMaximized).catch(() => {});
      win.onResized(() => {
        win.isMaximized().then(setIsMaximized).catch(() => {});
      }).then((fn) => {
        unlisten = fn;
      }).catch(() => {});
    }).catch(() => {});

    return () => {
      if (unlisten) unlisten();
    };
  }, []);

  if (!isTauri()) {
    return null;
  }

  const handleMinimize = (e) => {
    e.stopPropagation();
    appWindow?.minimize();
  };

  const handleToggleMaximize = (e) => {
    e.stopPropagation();
    appWindow?.toggleMaximize();
  };

  const handleClose = (e) => {
    e.stopPropagation();
    appWindow?.close();
  };

  const handleDoubleClick = () => {
    appWindow?.toggleMaximize();
  };

  const isMacPlatform = isMac();
  const isWinPlatform = isWindows();
  const logoSrc = theme === 'light' ? '/logo-black.svg' : '/logo-white.svg';

  return (
    <div
      className={`desktop-titlebar ${isMacPlatform ? 'is-mac' : ''} ${isWinPlatform ? 'is-windows' : ''}`}
      data-tauri-drag-region
      onDoubleClick={handleDoubleClick}
    >
      {/* macOS Traffic Lights Spacer */}
      {isMacPlatform && <div className="titlebar-mac-spacer" />}

      {/* Brand & Window Title */}
      <div className="titlebar-brand" data-tauri-drag-region>
        <img src={logoSrc} alt="Crescendo" className="titlebar-logo-img" />
        <span className="titlebar-name">Crescendo</span>
      </div>

      {/* Center Draggable Spacer */}
      <div className="titlebar-center" data-tauri-drag-region />

      {/* Windows & Linux Window Controls (Non-Mac) */}
      {!isMacPlatform && (
        <div className="titlebar-controls-win no-drag">
          <button
            type="button"
            className="titlebar-btn-win"
            onClick={handleMinimize}
            title="Minimize"
            aria-label="Minimize Window"
          >
            <svg width="10" height="1" viewBox="0 0 10 1">
              <path fill="currentColor" d="M0 0h10v1H0z" />
            </svg>
          </button>
          <button
            type="button"
            className="titlebar-btn-win"
            onClick={handleToggleMaximize}
            title={isMaximized ? "Restore Down" : "Maximize"}
            aria-label={isMaximized ? "Restore Window" : "Maximize Window"}
          >
            {isMaximized ? (
              <svg width="10" height="10" viewBox="0 0 10 10">
                <path fill="none" stroke="currentColor" strokeWidth="1" d="M2.5 1.5h6v6h-6zM1.5 3.5v5h5" />
              </svg>
            ) : (
              <svg width="10" height="10" viewBox="0 0 10 10">
                <path fill="none" stroke="currentColor" strokeWidth="1" d="M1.5 1.5h7v7h-7z" />
              </svg>
            )}
          </button>
          <button
            type="button"
            className="titlebar-btn-win close-btn"
            onClick={handleClose}
            title="Close"
            aria-label="Close Window"
          >
            <svg width="10" height="10" viewBox="0 0 10 10">
              <path stroke="currentColor" strokeWidth="1.2" d="M1 1l8 8M9 1L1 9" />
            </svg>
          </button>
        </div>
      )}
    </div>
  );
}
