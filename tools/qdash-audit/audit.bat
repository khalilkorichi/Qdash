@echo off
REM ── Qdash Audit Tool — Windows Launcher ────────────────────────────────────
REM Double-click this file from the project root to run the audit.
REM Or drag it onto a terminal and add --full to force a complete re-scan.

setlocal
SET SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo.
echo  ╔══════════════════════════════════════════════╗
echo  ║   Qdash Code ^& DB Health Audit Tool v1.0   ║
echo  ╚══════════════════════════════════════════════╝
echo.

REM Check Python availability
python --version >nul 2>&1
if errorlevel 1 (
    echo  ERROR: Python not found. Install Python 3.9+ and ensure it is on PATH.
    pause
    exit /b 1
)

REM Run the audit from the tools/qdash-audit directory
python audit.py %*

echo.
echo  Done. Open dashboard\index.html in your browser to view the results.
echo  (Tip: run with --full to force a complete re-scan of all files)
echo.
pause
