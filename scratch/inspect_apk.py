import os
import hashlib

apk_path = r"C:\Users\Khalil\antigravity\FinTrack-DZ-2026-05-29-30f3a\.build-outputs\New folder (2)\app-debug.apk"
size = os.path.getsize(apk_path)

sha256 = hashlib.sha256()
with open(apk_path, "rb") as f:
    for chunk in iter(lambda: f.read(4096), b""):
        sha256.update(chunk)

print(f"SIZE: {size}")
print(f"SHA256: {sha256.hexdigest().lower()}")
