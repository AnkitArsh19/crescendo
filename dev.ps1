# dev.ps1 — Crescendo Full-Stack Dev Launcher
# Starts Backend, AI/ML microservice, and Frontend each in their own terminal tab/window.
#
# Usage:
#   .\dev.ps1
#
# Prerequisites:
#   - Redis & PostgreSQL running (e.g. via `docker compose up -d postgres redis`)
#   - Java 21+, Node.js 18+, Python 3.10+

$root = $PSScriptRoot

$services = @(
    @{
        Title   = "Backend"
        Dir     = Join-Path $root "crescendo-backend"
        Command = '.\mvnw.cmd spring-boot:run'
    },
    @{
        Title   = "AIML"
        Dir     = Join-Path $root "crescendo-aiml"
        Command = 'if (Test-Path "venv\Scripts\Activate.ps1") { & "venv\Scripts\Activate.ps1" } elseif (Test-Path ".venv\Scripts\Activate.ps1") { & ".venv\Scripts\Activate.ps1" }; python -m uvicorn app.main:app --reload --port 8000'
    },
    @{
        Title   = "Frontend"
        Dir     = Join-Path $root "crescendo-frontend"
        Command = 'npm run dev'
    }
)

# Write a temp launcher script per service — avoids ALL quoting/delimiter issues with wt.exe
$tmpDir = $env:TEMP
$scripts = @()
foreach ($svc in $services) {
    $scriptPath = Join-Path $tmpDir "crescendo-$($svc.Title).ps1"
    $scriptContent = @"
Set-Location '$($svc.Dir)'
`$Host.UI.RawUI.WindowTitle = 'Crescendo $($svc.Title)'
$($svc.Command)
"@
    [System.IO.File]::WriteAllText($scriptPath, $scriptContent, [System.Text.Encoding]::UTF8)
    $scripts += @{ Title = $svc.Title; Script = $scriptPath; Dir = $svc.Dir }
}

$wtAvailable = $null -ne (Get-Command "wt.exe" -ErrorAction SilentlyContinue)

if ($wtAvailable) {
    Write-Host "Opening services in Windows Terminal tabs..." -ForegroundColor Cyan

    for ($i = 0; $i -lt $scripts.Count; $i++) {
        $s = $scripts[$i]
        if ($i -eq 0) {
            Start-Process "wt.exe" -ArgumentList "new-tab", "--title", $s.Title, "-d", $s.Dir, "powershell.exe", "-ExecutionPolicy", "Bypass", "-NoExit", "-File", $s.Script
        } else {
            Start-Sleep -Milliseconds 500
            Start-Process "wt.exe" -ArgumentList "-w", "0", "new-tab", "--title", $s.Title, "-d", $s.Dir, "powershell.exe", "-ExecutionPolicy", "Bypass", "-NoExit", "-File", $s.Script
        }
    }
} else {
    Write-Host "Windows Terminal not found — opening separate PowerShell windows..." -ForegroundColor Yellow
    foreach ($s in $scripts) {
        Start-Process "powershell.exe" -ArgumentList "-ExecutionPolicy", "Bypass", "-NoExit", "-File", $s.Script
        Start-Sleep -Milliseconds 500
    }
}

Write-Host ""
Write-Host "All services launched!" -ForegroundColor Green
Write-Host "  Backend  -> http://localhost:8080" -ForegroundColor Cyan
Write-Host "  AI/ML    -> http://localhost:8000" -ForegroundColor Green
Write-Host "  Frontend -> http://localhost:5173" -ForegroundColor Magenta
Write-Host ""
Write-Host "Tip: Close each terminal tab/window individually to stop a service." -ForegroundColor Gray