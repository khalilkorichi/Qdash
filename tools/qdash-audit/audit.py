#!/usr/bin/env python3
"""
audit.py — Qdash Code & Database Health Audit Tool
Entry point / CLI controller.

Usage:
    python audit.py
    python audit.py --full               # force re-scan all files
    python audit.py --report-only        # regenerate index.json only
    python audit.py --project-root <path>
    python audit.py --serve              # run local HTTP API server on port 8080

Outputs:
    - reports/scan_<timestamp>.json      (full report, never overwritten)
    - reports/index.json                 (rolling scan history)
    - Prints summary to stdout
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import http.server
import socketserver
import urllib.parse
from pathlib import Path

# ─── Path setup ─────────────────────────────────────────────────────────────
# audit.py lives at tools/qdash-audit/audit.py
# project root is two levels up
_AUDIT_ROOT    = Path(__file__).resolve().parent
_PROJECT_ROOT  = _AUDIT_ROOT.parent.parent

# Add audit root to sys.path so relative imports work
sys.path.insert(0, str(_AUDIT_ROOT))

from use_cases import (
    RunFullScanUseCase,
    GetScanHistoryUseCase,
    GetCurrentIssuesUseCase,
    GetDatabaseHealthUseCase,
)
from models import SEVERITY_ORDER, SEVERITY_COLORS


# ─── Terminal colours (ANSI, Windows 10+ compatible) ─────────────────────────
def _ansi(code: str) -> str:
    return f"\033[{code}m"


RESET  = _ansi("0")
BOLD   = _ansi("1")
RED    = _ansi("91")
ORANGE = _ansi("93")
YELLOW = _ansi("33")
BLUE   = _ansi("94")
GRAY   = _ansi("37")
GREEN  = _ansi("92")
CYAN   = _ansi("96")

SEV_COLOR = {
    "CRITICAL": RED,
    "HIGH":     ORANGE,
    "MEDIUM":   YELLOW,
    "LOW":      BLUE,
    "INFO":     GRAY,
}

def _enable_ansi_on_windows() -> None:
    """Enable VT100 escape codes and UTF-8 output on Windows 10+ cmd/powershell."""
    if sys.platform == "win32":
        try:
            import ctypes
            kernel32 = ctypes.windll.kernel32
            kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
        except Exception:
            pass
        # Force UTF-8 output so box-drawing chars work
        try:
            import io
            sys.stdout = io.TextIOWrapper(
                sys.stdout.buffer, encoding='utf-8', errors='replace'
            )
        except Exception:
            pass


# ─── CLI ─────────────────────────────────────────────────────────────────────
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Qdash Code & Database Health Audit Tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument(
        "--full", action="store_true",
        help="Force re-scan all files (ignore hash cache)"
    )
    p.add_argument(
        "--report-only", action="store_true",
        help="Regenerate index.json from existing reports without scanning"
    )
    p.add_argument(
        "--project-root", default=str(_PROJECT_ROOT),
        help=f"Path to the Qdash repo root (default: {_PROJECT_ROOT})"
    )
    p.add_argument(
        "--audit-root", default=str(_AUDIT_ROOT),
        help=f"Path to tools/qdash-audit/ (default: {_AUDIT_ROOT})"
    )
    p.add_argument(
        "--json", action="store_true",
        help="Print the scan report JSON to stdout (machine-readable)"
    )
    p.add_argument(
        "--history", action="store_true",
        help="Print scan history summary and exit"
    )
    p.add_argument(
        "--serve", action="store_true",
        help="Run local HTTP API server on port 8080"
    )
    return p.parse_args()


# ─── Print helpers ────────────────────────────────────────────────────────────
def _print_header(project_root: str) -> None:
    print(f"\n{BOLD}{CYAN}╔══════════════════════════════════════════════╗{RESET}")
    print(f"{BOLD}{CYAN}║   Qdash Code & DB Health Audit Tool v1.0    ║{RESET}")
    print(f"{BOLD}{CYAN}╚══════════════════════════════════════════════╝{RESET}")
    print(f"  Project: {BOLD}{project_root}{RESET}\n")


def _print_severity_bar(issues_by_severity: dict[str, int]) -> None:
    total = sum(issues_by_severity.values())
    print(f"  {'Issues by severity':25s}", end="")
    for sev in SEVERITY_ORDER:
        count = issues_by_severity.get(sev, 0)
        color = SEV_COLOR.get(sev, "")
        if count:
            print(f"  {color}{sev}:{RESET} {BOLD}{count}{RESET}", end="")
    print(f"\n  {'Total issues':25s}  {BOLD}{total}{RESET}")


def _print_report_summary(report) -> None:
    print(f"\n{BOLD}Scan complete{RESET}")
    print(f"  Files scanned:  {report.total_files_scanned}")
    print(f"  Duration:       {report.duration_seconds}s")
    print(f"  DB schema ver:  {report.schema_version}")
    _print_severity_bar(report.issues_by_severity)

    new  = len(report.new_issue_ids)
    res  = len(report.resolved_issue_ids)
    if new:
        print(f"\n  {RED}▲ {new} new issue(s) introduced{RESET}")
    if res:
        print(f"  {GREEN}▼ {res} issue(s) resolved{RESET}")

    if report.total_issues == 0:
        print(f"\n  {GREEN}{BOLD}✓ No issues found!{RESET}")
        return

    print(f"\n{BOLD}Issue list{RESET} (grouped by severity):\n")
    grouped: dict[str, list] = {sev: [] for sev in SEVERITY_ORDER}
    for issue in report.issues:
        grouped.setdefault(issue.severity, []).append(issue)

    for sev in SEVERITY_ORDER:
        issues = grouped.get(sev, [])
        if not issues:
            continue
        color = SEV_COLOR.get(sev, "")
        print(f"  {color}{BOLD}── {sev} ({len(issues)}) ──{RESET}")
        for iss in issues:
            loc = f":{iss.line_start}" if iss.line_start else ""
            print(f"    [{iss.rule_id}] {iss.title}")
            print(f"           {GRAY}{iss.file_path}{loc}{RESET}")
            if iss.user_symptom:
                print(f"           ↳ {iss.user_symptom}")
        print()


def _print_history(history) -> None:
    if not history:
        print("  No scans in history yet.")
        return
    print(f"\n{BOLD}Scan History{RESET} ({len(history)} scans)\n")
    print(f"  {'Timestamp':26s} {'Files':>6s} {'Issues':>7s} {'New':>5s} {'Resolved':>9s} {'Duration':>9s}")
    print("  " + "-" * 70)
    for s in history:
        dt  = s.started_at[:19].replace("T", " ")
        new = f"+{s.new_issues}" if s.new_issues else "  0"
        res = f"-{s.resolved_issues}" if s.resolved_issues else "  0"
        print(f"  {dt:26s} {s.total_files_scanned:6d} {s.total_issues:7d} "
              f"{new:>5s} {res:>9s} {s.duration_seconds:>8.1f}s")


# ─── Dashboard data writer ────────────────────────────────────────────────────
def _write_dashboard_data(audit_root: str, report) -> None:
    """Write dashboard/data.js with embedded scan data for offline file:// access."""
    history = GetScanHistoryUseCase(audit_root).run()
    history_dicts = [s.to_dict() for s in history]
    report_dict   = report.to_dict() if hasattr(report, "to_dict") else report

    data_js_path = Path(audit_root) / "dashboard" / "data.js"
    data_js_path.parent.mkdir(parents=True, exist_ok=True)

    payload = json.dumps(
        {"current": report_dict, "history": history_dicts},
        ensure_ascii=False, indent=2,
    )
    data_js_path.write_text(
        "// Auto-generated by audit.py\nwindow.AUDIT_DATA = " + payload + ";\n",
        encoding="utf-8",
    )
    print("  [+] Dashboard data written to dashboard/data.js")


# ─── HTTP API Server ─────────────────────────────────────────────────────────
class QdashAuditHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        # Enable CORS for file:// access
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path

        if path == '/api/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json; charset=utf-8')
            self.end_headers()
            
            from release_manager import ReleaseManager
            rm = ReleaseManager()
            versions = rm.read_version_info(str(_PROJECT_ROOT))
            status = rm.get_status()
            
            response = {
                "server_status": "online",
                "release_status": status,
                "version_info": versions
            }
            self.wfile.write(json.dumps(response, ensure_ascii=False).encode('utf-8'))
            return

        elif path == '/api/release/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json; charset=utf-8')
            self.end_headers()
            
            from release_manager import ReleaseManager
            status = ReleaseManager().get_status()
            self.wfile.write(json.dumps(status, ensure_ascii=False).encode('utf-8'))
            return

        # Serve static dashboard files
        audit_dir = Path(_AUDIT_ROOT)
        if path == '/' or path == '/index.html':
            path_to_serve = audit_dir / 'dashboard' / 'index.html'
        elif path in ['/app.js', '/styles.css', '/data.js']:
            path_to_serve = audit_dir / 'dashboard' / path.lstrip('/')
        elif path.startswith('/reports/'):
            path_to_serve = audit_dir / path.lstrip('/')
        else:
            super().do_GET()
            return

        if path_to_serve.exists():
            self.send_response(200)
            if path_to_serve.suffix == '.html':
                self.send_header('Content-type', 'text/html; charset=utf-8')
            elif path_to_serve.suffix == '.css':
                self.send_header('Content-type', 'text/css; charset=utf-8')
            elif path_to_serve.suffix == '.js':
                self.send_header('Content-type', 'application/javascript; charset=utf-8')
            elif path_to_serve.suffix == '.json':
                self.send_header('Content-type', 'application/json; charset=utf-8')
            self.end_headers()
            self.wfile.write(path_to_serve.read_bytes())
        else:
            self.send_error(404, "File not found")

    def do_POST(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path

        if path == '/api/release':
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length).decode('utf-8')
            
            try:
                params = json.loads(post_data)
            except Exception:
                params = {}
                
            release_notes = params.get("releaseNotes", "تحديث عام وإصلاحات برمجية.")
            
            from release_manager import ReleaseManager
            rm = ReleaseManager()
            started = rm.start_release(str(_PROJECT_ROOT), release_notes)
            
            self.send_response(200 if started else 400)
            self.send_header('Content-type', 'application/json; charset=utf-8')
            self.end_headers()
            
            response = {
                "success": started,
                "message": "بدأت عملية التحديث بنجاح." if started else "عملية التحديث قيد التشغيل بالفعل."
            }
            self.wfile.write(json.dumps(response, ensure_ascii=False).encode('utf-8'))
            return
            
        super().do_POST()


def run_server(project_root: str, audit_root: str):
    PORT = 8080
    
    class ThreadingHTTPServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
        daemon_threads = True

    server_address = ('', PORT)
    handler = QdashAuditHTTPRequestHandler
    ThreadingHTTPServer.allow_reuse_address = True
    
    try:
        httpd = ThreadingHTTPServer(server_address, handler)
        print(f"\n{BOLD}{GREEN}  [✓] الخادم المحلي نشط ويعمل على الرابط التالي:{RESET}")
        print(f"      {CYAN}http://localhost:{PORT}/index.html{RESET}\n")
        print("  اضغط Ctrl+C لإيقاف الخادم.")
        httpd.serve_forever()
    except Exception as e:
        print(f"\n  {RED}خطأ أثناء بدء الخادم:{RESET} {e}")
        sys.exit(1)


# ─── Main ─────────────────────────────────────────────────────────────────────
def main() -> int:
    _enable_ansi_on_windows()
    args = parse_args()

    project_root = str(Path(args.project_root).resolve())
    audit_root   = str(Path(args.audit_root).resolve())

    # ── Server Mode ──────────────────────────────────────────────────────────
    if args.serve:
        _print_header(project_root)
        run_server(project_root, audit_root)
        return 0

    _print_header(project_root)

    # ── History mode ─────────────────────────────────────────────────────────
    if args.history:
        history = GetScanHistoryUseCase(audit_root).run()
        _print_history(history)
        return 0

    # ── Report-only mode ─────────────────────────────────────────────────────
    if args.report_only:
        print("  report-only mode: regenerating index.json…")
        report = GetCurrentIssuesUseCase(audit_root).run()
        if report:
            _print_report_summary(report)
        else:
            print("  No reports found. Run a full scan first.")
        return 0

    # ── Full scan ─────────────────────────────────────────────────────────────
    print(f"  Mode: {'FULL (forced)' if args.full else 'incremental'}\n")

    use_case = RunFullScanUseCase(
        project_root=project_root,
        audit_root=audit_root,
        force_full=args.full,
    )
    report, report_file = use_case.run()

    if args.json:
        print(json.dumps(report.to_dict(), indent=2, ensure_ascii=False))
        return 0

    _print_report_summary(report)

    reports_path   = Path(audit_root) / "reports" / report_file
    dashboard_path = Path(audit_root) / "dashboard" / "index.html"

    # Write embedded data.js so dashboard works via file://
    _write_dashboard_data(audit_root, report)

    print(f"\n  {BOLD}Report saved:{RESET} {reports_path}")
    print(f"  {BOLD}Dashboard:   {RESET} {dashboard_path}")
    print(f"\n  {GREEN}Open dashboard in browser (works offline):{RESET}")
    print(f"  {CYAN}{dashboard_path}{RESET}")
    print(f"  {GRAY}أو لتفعيل عملية التحديث التلقائي، شغّل الخادم:{RESET} {CYAN}python audit.py --serve{RESET}\n")

    # Exit 0 = ran successfully (having issues is normal, not an error)
    return 0


if __name__ == "__main__":
    sys.exit(main())
