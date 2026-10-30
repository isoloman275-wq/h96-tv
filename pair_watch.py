#!/usr/bin/env python3
"""Watch the H96 screen for the Google-TV-app pairing dialog and accept it via ADB."""
import subprocess, time, re, sys

ADB = "/mnt/c/Users/Admin/Downloads/PhoenixSuit_V1.19/PhoenixSuit_V1.19/adb.exe"
BOX = "<box-ip>:5555"  # set your box IP
ACCEPT_WORDS = ("allow", "pair", "connect", "ok", "yes", "trust")

def sh(*args):
    return subprocess.run([ADB, "-s", BOX] + list(args),
                          capture_output=True, timeout=25).stdout.decode(errors="ignore")

def dump():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    return sh("shell", "cat", "/sdcard/ui.xml")

def find_button(xml):
    # nodes with acceptable text AND tappable bounds
    best = None
    for m in re.finditer(r'<node[^>]*/?>', xml):
        n = m.group(0)
        tm = re.search(r'text="([^"]*)"', n) or re.search(r'content-desc="([^"]*)"', n)
        if not tm:
            continue
        label = tm.group(1).strip().lower()
        if not any(w == label or w in label for w in ACCEPT_WORDS):
            continue
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if not bm:
            continue
        x1, y1, x2, y2 = map(int, bm.groups())
        if (x2 - x1) < 40 or (y2 - y1) < 30:   # ignore slivers
            continue
        best = ((x1 + x2) // 2, (y1 + y2) // 2, tm.group(1))
    return best

print("watching for pairing dialog...", flush=True)
deadline = time.time() + 480
seen_dialog = False
while time.time() < deadline:
    xml = dump()
    btn = find_button(xml)
    low = xml.lower()
    dialog_present = any(w in low for w in ("pair", "allow") ) and "freetv" not in low[:200]
    if btn:
        print(f"DIALOG BUTTON FOUND: '{btn[2]}' at {btn[0]},{btn[1]} -> tapping", flush=True)
        sh("shell", "input", "tap", str(btn[0]), str(btn[1]))
        seen_dialog = True
        time.sleep(3)
        # second confirm sometimes appears
        b2 = find_button(dump())
        if b2:
            print(f"second confirm '{b2[2]}' -> tapping", flush=True)
            sh("shell", "input", "tap", str(b2[0]), str(b2[1]))
        print("checking socket state...", flush=True)
        out = sh("shell", "netstat", "-tnp")
        est = [l for l in out.splitlines() if ":6466" in l or ":6467" in l]
        print("\n".join(est) if est else "(no remote sockets)", flush=True)
        if any("ESTABLISHED" in l for l in est):
            print("PAIRING SUCCESS - remote established", flush=True)
            sys.exit(0)
    elif dialog_present and not seen_dialog:
        print("dialog-ish screen detected but no known button yet", flush=True)
    time.sleep(2.5)
print("timeout - no pairing completed", flush=True)
