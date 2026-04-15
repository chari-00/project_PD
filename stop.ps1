# stop.ps1
Write-Host "Stopping Plagiarism Detector System..."

if (Test-Path "backend.pid") {
    $backendPid = Get-Content "backend.pid"
    Write-Host "Stopping backend (PID: $backendPid)..."
    # Kill the entire process tree to ensure maven and java are both stopped
    taskkill /PID $backendPid /T /F 2>$null
    Remove-Item "backend.pid" -ErrorAction SilentlyContinue
} else {
    Write-Host "Backend PID file not found."
}

if (Test-Path "frontend.pid") {
    $frontendPid = Get-Content "frontend.pid"
    Write-Host "Stopping frontend (PID: $frontendPid)..."
    taskkill /PID $frontendPid /T /F 2>$null
    Remove-Item "frontend.pid" -ErrorAction SilentlyContinue
} else {
    Write-Host "Frontend PID file not found."
}

# Fallback: kill processes by name or port if they were left orphaned
Write-Host "Cleaning up any orphaned Python HTTP servers on port 5500..."
$orphanedPython = Get-NetTCPConnection -LocalPort 5500 -ErrorAction SilentlyContinue
if ($orphanedPython) {
    Stop-Process -Id $orphanedPython.OwningProcess -Force -ErrorAction SilentlyContinue
}

Write-Host "System stopped successfully."
