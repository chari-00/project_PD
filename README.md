# Plagiarism Detector - Run and Integrate Guide

This workspace now has a runnable Spring Boot backend and a standalone frontend UI.

## Current Structure

```
PD/
├── backend/
│   ├── .env
│   ├── .gitignore
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/plagiarism/
│       │   ├── PlagiarismDetectorApplication.java
│       │   ├── config/CorsConfig.java
│       │   ├── controller/PlagiarismController.java
│       │   ├── model/
│       │   │   ├── CompareRequest.java
│       │   │   └── PlagiarismResult.java
│       │   └── service/
│       │       ├── TextPreprocessorService.java
│       │       ├── LcsEngineService.java
│       │       ├── PlagiarismService.java
│       │       └── GeminiVlmService.java
│       └── resources/application.properties
└── frontend/
     ├── .gitignore
     ├── index.html
     └── README.md
```

## 1) Prerequisites

- Java 17+
- Maven 3.8+
- Python 3.x (only needed to serve frontend locally)

If `mvn` is not recognized on your machine, install Maven and restart terminal.

## 2) API Key (.env)

Gemini key is loaded from environment variables or from `.env`.

Current `.env` location in this workspace:
- `backend/.env`

Expected format:

```env
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
GEMINI_MODEL=gemini-2.5-flash
```

`GeminiVlmService` searches common locations and prioritizes `backend/.env`.

## 3) Run the System (New Method)

We have added unified scripts to easily start and stop the entire system (backend and frontend).

### Starting the System

From the workspace root (`PD`), simply execute:

```powershell
.\start.ps1
```

This script will:
1. Start the Spring Boot backend via Maven (minimized window).
2. Start the Python web server for the frontend (minimized window).
3. Expose the Backend on http://localhost:8080 and Frontend on http://localhost:5500.

### Stopping the System

When you are done, run the following from the workspace root:

```powershell
.\stop.ps1
```

This will gracefully terminate both the backend and frontend servers.

---

## 4) Run Manually (Old Method)

**Backend:**
From workspace root (`PD`):

```bash
cd backend
mvn spring-boot:run
```

If `mvn` is not available on PATH, use the local Maven binary in `tools/`:

```bash
cd backend
..\tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Backend base URL:

```text
http://localhost:8080/api
```

Quick health check:

```text
http://localhost:8080/api/health
```

## 4) Run Frontend

In a second terminal:

```bash
cd frontend
python -m http.server 5500
```

Open:

```text
http://localhost:5500/index.html
```

## 5) Integrate Frontend with Backend

In the UI:

1. Find the `Backend API` input.
2. Set it to:

```text
http://localhost:8080/api
```

3. Click `Connect`.

The value is stored in browser localStorage key `PD_API_BASE`.

## 6) Verify End-to-End Flow

1. Confirm health badge in UI shows backend connected.
2. Try text mode:
    - paste two docs
    - click `Analyze Text`
3. Try visual mode:
    - upload two PDF/image files
    - click `Analyze Visual Docs`

## API Endpoints

- `GET /api/health`
- `POST /api/compare`
- `POST /api/compare/files`
- `POST /api/vlm/extract`
- `POST /api/compare/visual`
- `POST /api/tokenize`
