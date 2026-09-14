; Crescendo NSIS Custom Installer & Uninstaller Hooks
; Ensures clean removal of deep-link protocol associations and WebView2 user data cache.

!macro NSIS_HOOK_PREINSTALL
  ; Runs before file extraction and registry setup
!macroend

!macro NSIS_HOOK_POSTINSTALL
  ; Runs after files, registry keys, and shortcuts are created
!macroend

!macro NSIS_HOOK_PREUNINSTALL
  ; Runs before removing files
!macroend

!macro NSIS_HOOK_POSTUNINSTALL
  ; 1. Remove crescendo:// URL protocol handler from registry to prevent orphan associations
  DeleteRegKey HKCU "Software\Classes\crescendo"

  ; 2. Clean up WebView2 user data, IndexedDB, cookies, and app cache
  RMDir /r "$LOCALAPPDATA\run.crescendo.desktop"
  RMDir /r "$LOCALAPPDATA\crescendo"
!macroend
