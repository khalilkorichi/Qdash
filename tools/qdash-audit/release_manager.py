"""
release_manager.py — Orchestrates application release workflow.
Performs the following steps:
1. Validates git status and branch.
2. Bumps versionCode, versionName, and UPDATE_IDENTITY in app/build.gradle.kts.
3. Bumps version fields in update.json.
4. Executes gradle build (assembleDebug) and captures real-time console log.
5. Copies built APK to target directory.
6. Updates update.json metadata (size, hash, timestamp, release notes).
7. Runs git commands to add, commit, and push.
8. Verifies GitHub raw CDN for the new update.
"""
from __future__ import annotations

import os
import re
import sys
import time
import json
import hashlib
import threading
import subprocess
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional


class ReleaseManager:
    _instance: Optional[ReleaseManager] = None
    _lock = threading.Lock()

    def __new__(cls, *args, **kwargs):
        with cls._lock:
            if cls._instance is None:
                cls._instance = super().__new__(cls)
                cls._instance._init_manager()
            return cls._instance

    def _init_manager(self):
        self.state = {
            "status": "idle",  # idle, running, completed, failed
            "current_step": 0,
            "total_steps": 7,
            "step_title": "",
            "logs": [],
            "error_message": "",
            "progress_percent": 0,
            "version_info": {},
        }
        self.lock = threading.Lock()
        self._thread: Optional[threading.Thread] = None

    def get_status(self) -> Dict[str, Any]:
        with self.lock:
            return dict(self.state)

    def log(self, message: str):
        timestamp = datetime.now().strftime("%H:%M:%S")
        line = f"[{timestamp}] {message}"
        print(f"Update Process: {message}")
        with self.lock:
            self.state["logs"].append(line)

    def update_step(self, step_idx: int, title: str, progress: int = None):
        with self.lock:
            self.state["current_step"] = step_idx
            self.state["step_title"] = title
            if progress is not None:
                self.state["progress_percent"] = progress
            else:
                self.state["progress_percent"] = int((step_idx / self.state["total_steps"]) * 100)
        self.log(f"--- Step {step_idx}/{self.state['total_steps']}: {title} ---")

    def set_failed(self, error_msg: str):
        with self.lock:
            self.state["status"] = "failed"
            self.state["error_message"] = error_msg
        self.log(f"❌ ERROR: {error_msg}")

    def set_completed(self):
        with self.lock:
            self.state["status"] = "completed"
            self.state["progress_percent"] = 100
        self.log("✅ Release process completed successfully!")

    def start_release(self, project_root: str, release_notes: str) -> bool:
        with self.lock:
            if self.state["status"] == "running":
                return False
            self.state["status"] = "running"
            self.state["current_step"] = 0
            self.state["step_title"] = "Initializing..."
            self.state["logs"] = []
            self.state["error_message"] = ""
            self.state["progress_percent"] = 0

        self._thread = threading.Thread(
            target=self._run_release_thread,
            args=(project_root, release_notes),
            daemon=True
        )
        self._thread.start()
        return True

    def get_github_repo_info(self, project_root: str) -> tuple[str, str]:
        try:
            res = subprocess.run(
                ["git", "remote", "get-url", "origin"],
                cwd=project_root, capture_output=True, text=True, timeout=5
            )
            url = res.stdout.strip()
            match = re.search(r'github\.com[:/](?P<user>[^/]+)/(?P<repo>[^.]+)(?:\.git)?', url)
            if match:
                return match.group("user"), match.group("repo")
        except Exception:
            pass
        return "khalilkorichi", "Qdash"

    def read_version_info(self, project_root: str) -> Dict[str, Any]:
        gradle_path = Path(project_root) / "app" / "build.gradle.kts"
        update_json_path = Path(project_root) / "update.json"

        info = {
            "gradle_version_code": None,
            "gradle_version_name": None,
            "gradle_update_identity": None,
            "json_version_code": None,
            "json_version_name": None,
            "json_update_identity": None,
        }

        if gradle_path.exists():
            content = gradle_path.read_text(encoding="utf-8")
            code_match = re.search(r'versionCode\s*=\s*(\d+)', content)
            name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
            ident_match = re.search(r'buildConfigField\("Long",\s*"UPDATE_IDENTITY",\s*"(\d+)L"\)', content)

            if code_match: info["gradle_version_code"] = int(code_match.group(1))
            if name_match: info["gradle_version_name"] = name_match.group(1)
            if ident_match: info["gradle_update_identity"] = int(ident_match.group(1))

        if update_json_path.exists():
            try:
                data = json.loads(update_json_path.read_text(encoding="utf-8"))
                info["json_version_code"] = data.get("versionCode")
                info["json_version_name"] = data.get("versionName")
                info["json_update_identity"] = data.get("updateIdentity")
            except Exception:
                pass

        return info

    def _run_release_thread(self, project_root: str, release_notes: str):
        try:
            self.log("Starting release process...")
            self.log(f"Project root: {project_root}")

            # ── Step 1: Validate Git & Workspace ─────────────────────────────
            self.update_step(1, "التحقق من حالة المستودع (Git)")
            
            # Check git status
            try:
                git_status = subprocess.run(
                    ["git", "status", "--porcelain"],
                    cwd=project_root, capture_output=True, text=True, check=True
                )
                # We can allow clean build even if there are edits, but warn
                untracked = len(git_status.stdout.strip().splitlines())
                if untracked > 0:
                    self.log(f"⚠️ يوجد ملفات معدلة غير مسجلة ({untracked} ملفات). سيتم تسجيلها لاحقاً.")
                else:
                    self.log("مستودع Git نظيف وجاهز.")
            except Exception as e:
                self.log(f"⚠️ تحذير: فشل فحص حالة git: {e}")

            # Read current version info
            info = self.read_version_info(project_root)
            self.log(f"نسخة Gradle الحالية: Code={info['gradle_version_code']}, Name={info['gradle_version_name']}, Identity={info['gradle_update_identity']}")
            self.log(f"نسخة update.json الحالية: Code={info['json_version_code']}, Name={info['json_version_name']}, Identity={info['json_update_identity']}")

            if not info["gradle_version_code"] or not info["gradle_version_name"] or not info["gradle_update_identity"]:
                raise Exception("فشل العثور على معلومات الإصدار في ملف app/build.gradle.kts")

            # Calculate next versions
            new_code = info["gradle_version_code"] + 1
            new_name = f"1.0.0.{new_code}"
            new_identity = info["gradle_update_identity"] + 1

            self.log(f"🚀 الإصدار الجديد المقترح: Code={new_code}, Name={new_name}, Identity={new_identity}")

            # ── Step 2: Bump Version Numbers ──────────────────────────────
            self.update_step(2, "تحديث ملفات الإعدادات ورقم الإصدار الجديد")
            gradle_file = Path(project_root) / "app" / "build.gradle.kts"
            gradle_content = gradle_file.read_text(encoding="utf-8")

            # Modify gradle
            gradle_content = re.sub(
                r'(versionCode\s*=\s*)(\d+)',
                f'\\g<1>{new_code}',
                gradle_content
            )
            gradle_content = re.sub(
                r'(versionName\s*=\s*)"([^"]+)"',
                f'\\g<1>"{new_name}"',
                gradle_content
            )
            gradle_content = re.sub(
                r'(buildConfigField\("Long",\s*"UPDATE_IDENTITY",\s*")(\d+)(L"\))',
                f'\\g<1>{new_identity}\\g<3>',
                gradle_content
            )
            gradle_file.write_text(gradle_content, encoding="utf-8")
            self.log("تمت كتابة أرقام الإصدارات الجديدة في app/build.gradle.kts بنجاح.")

            # ── Step 3: Build APK ──────────────────────────────────────────
            self.update_step(3, "بناء التطبيق وتشغيل Gradle")
            self.log("جاري تشغيل gradlew.bat assembleDebug... قد يستغرق هذا دقيقة أو أكثر.")

            # Build gradlew.bat command
            gradlew_path = Path(project_root) / "gradlew.bat"
            if not gradlew_path.exists():
                raise Exception("لم يتم العثور على gradlew.bat في جذر المشروع!")

            # Run build capturing stdout line by line
            proc = subprocess.Popen(
                ["cmd.exe", "/c", "gradlew.bat", "assembleDebug"],
                cwd=project_root,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
            )

            # Read build output stream
            if proc.stdout:
                for line in proc.stdout:
                    stripped = line.strip()
                    if stripped:
                        # Log build events to dashboard logs
                        self.log(f"Gradle: {stripped}")

            return_code = proc.wait()
            if return_code != 0:
                raise Exception(f"فشلت عملية البناء بـ Gradle! رمز الخروج: {return_code}")

            self.log("انتهت عملية بناء Gradle بنجاح.")

            # ── Step 4: Copy Built APK ─────────────────────────────────────
            self.update_step(4, "نسخ ملف APK الناتج وتحديث البنية")
            src_apk = Path(project_root) / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
            dest_apk = Path(project_root) / ".build-outputs" / "New folder (2)" / "app-debug.apk"

            if not src_apk.exists():
                raise Exception(f"ملف APK الناتج غير موجود في المسار: {src_apk}")

            # Ensure parent directories exist
            dest_apk.parent.mkdir(parents=True, exist_ok=True)
            
            # Copy file
            dest_apk.write_bytes(src_apk.read_bytes())
            self.log(f"تم نسخ ملف APK بنجاح إلى: {dest_apk}")

            # Get size and sha256
            apk_size = dest_apk.stat().st_size
            sha256_hash = hashlib.sha256()
            with open(dest_apk, "rb") as f:
                for byte_block in iter(lambda: f.read(4096), b""):
                    sha256_hash.update(byte_block)
            apk_sha = sha256_hash.hexdigest().lower()

            self.log(f"معلومات APK: الحجم = {apk_size} بايت | SHA256 = {apk_sha}")

            # ── Step 5: Update update.json ──────────────────────────────────
            self.update_step(5, "تحديث ملف معلومات التحديثات update.json")
            update_json_file = Path(project_root) / "update.json"
            
            user, repo = self.get_github_repo_info(project_root)
            apk_url = f"https://raw.githubusercontent.com/{user}/{repo}/main/.build-outputs/New%20folder%20(2)/app-debug.apk?v={new_code}"

            update_data = {
                "versionCode": new_code,
                "versionName": new_name,
                "updateIdentity": new_identity,
                "publishedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "apkUrl": apk_url,
                "apkSize": apk_size,
                "apkSha256": apk_sha,
                "releaseNotes": release_notes,
                "minSdkVersion": 24,
                "forceUpdate": False,
                "mandatory": False
            }

            update_json_file.write_text(json.dumps(update_data, indent=2, ensure_ascii=False), encoding="utf-8")
            self.log("تم تحديث وحفظ ملف update.json بنجاح.")

            # ── Step 6: Git Commit & Push ───────────────────────────────────
            self.update_step(6, "تسجيل التغييرات والرفع إلى GitHub")
            
            # Git add
            subprocess.run(
                ["git", "add", "app/build.gradle.kts", "update.json", ".build-outputs/New folder (2)/app-debug.apk"],
                cwd=project_root, check=True, capture_output=True
            )
            self.log("تمت إضافة الملفات للتتبع (git add).")

            # Git commit
            commit_msg = f"release: v{new_name} - {release_notes.splitlines()[0]}"
            subprocess.run(
                ["git", "commit", "-m", commit_msg],
                cwd=project_root, check=True, capture_output=True
            )
            self.log(f"تم تسجيل التغييرات: commit -m \"{commit_msg}\"")

            # Git push
            self.log("جاري الرفع إلى المستودع البعيد: git push... قد يستغرق هذا بضع ثوانٍ.")
            git_push = subprocess.run(
                ["git", "push"],
                cwd=project_root, capture_output=True, text=True
            )
            if git_push.returncode != 0:
                self.log(f"⚠️ فشل الرفع التلقائي (قد لا تملك صلاحية أو المستودع البعيد يحتوي تعديلات جديدة): {git_push.stderr}")
                self.log("تنبيه: يجب عليك الرفع يدوياً عبر سطر الأوامر (git push).")
            else:
                self.log("تم الرفع بنجاح إلى GitHub!")

            # ── Step 7: Verify Update Presence on CDN ─────────────────────────
            self.update_step(7, "التحقق من ظهور التحديث على شبكة توزيع المحتوى (CDN)")
            cdn_url = f"https://raw.githubusercontent.com/{user}/{repo}/main/update.json?t={int(time.time())}"
            self.log(f"جاري التحقق من التحديث على CDN: {cdn_url}")

            verified = False
            for attempt in range(1, 13):  # Check every 10 seconds for 2 minutes
                self.log(f"المحاولة {attempt}/12 للتحقق من CDN...")
                try:
                    req = urllib.request.Request(
                        cdn_url,
                        headers={'Cache-Control': 'no-cache', 'Pragma': 'no-cache'}
                    )
                    with urllib.request.urlopen(req, timeout=10) as response:
                        body = response.read().decode("utf-8")
                        remote_data = json.loads(body)
                        remote_code = remote_data.get("versionCode")
                        self.log(f"تم الاتصال بـ CDN. رقم الإصدار الحالي البعيد: {remote_code}")
                        if remote_code == new_code:
                            self.log("🎉 رائع! تم التحقق بنجاح من أن التحديث الجديد أصبح متاحاً الآن لجميع المستخدمين.")
                            verified = True
                            break
                        else:
                            self.log("النسخة البعيدة لم تتحدث بعد على شبكة GitHub (قد يستغرق بعض الوقت للتحديث).")
                except Exception as e:
                    self.log(f"خطأ أثناء الاتصال بـ CDN: {e}")
                
                if attempt < 12:
                    time.sleep(10)

            if not verified:
                self.log("⚠️ انتهى وقت فحص CDN ولكن لم تظهر التغييرات بعد. قد تستغرق CDN من دقيقة إلى 5 دقائق للتحديث بالكامل.")

            # Update local versions in state
            with self.lock:
                self.state["version_info"] = {
                    "versionCode": new_code,
                    "versionName": new_name,
                    "updateIdentity": new_identity,
                }

            self.set_completed()

        except Exception as e:
            self.set_failed(str(e))
