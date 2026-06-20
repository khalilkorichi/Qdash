# -*- coding: utf-8 -*-
with open(r"c:\Users\Khalil\antigravity\FinTrack-DZ-2026-05-29-30f3a\app\src\main\java\com\example\presentation\transactions\AddTransactionScreen.kt", "rb") as f:
    content = f.read()

# Let's find handleNumpadKey in the file
index = content.find(b"private fun handleNumpadKey")
if index != -1:
    snippet = content[index:index+1000]
    print(snippet.decode("utf-8", errors="replace"))
    print("\n--- HEX BYTES ---")
    print(snippet[:300])
else:
    print("handleNumpadKey not found")
