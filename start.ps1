# start.ps1
Write-Host "Starting Plagiarism Detector System..."

# Check requirements
if (-not (Get-Command "python" -ErrorAction SilentlyContinue)) {
    Write-Warning "Python is not installed or not in PATH. Required for frontend server."
    exit 1
}

# Start backend
Write-Host "Starting backend server via Maven..."
$backendProcess = Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd backend && ..\tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run" -WindowStyle Minimized -PassThru
$backendProcess.Id | Out-File -FilePath "backend.pid"

# Wait a few seconds for backend to initialize
Start-Sleep -Seconds 5

# Start frontend
Write-Host "Starting frontend server..."
$frontendProcess = Start-Process -FilePath "python" -ArgumentList "-m http.server 5500" -WorkingDirectory "frontend" -WindowStyle Minimized -PassThru
$frontendProcess.Id | Out-File -FilePath "frontend.pid"

Write-Host "System started!"
Write-Host "Backend is running on http://localhost:8080"
Write-Host "Frontend is running on http://localhost:5500"
Write-Host "Run .\stop.ps1 to stop all services."
