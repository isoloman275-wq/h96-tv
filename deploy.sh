#!/bin/bash
# Deploy NZ1 Free TV launcher to the H96 Max over WiFi ADB.
# Usage: ~/h96-tv/deploy.sh [box-ip]   (default 192.168.1.X — set your box IP)
set -u
BOX="${1:-<set-box-ip>}"
ADB="/mnt/c/Users/Admin/Downloads/PhoenixSuit_V1.19/PhoenixSuit_V1.19/adb.exe"
APK="~/h96-tv/app/build/outputs/apk/release/app-release.apk"

[ -f "$APK" ] || { echo "NO APK at $APK — run the build first"; exit 1; }

"$ADB" connect "$BOX:5555" >/dev/null
echo "== device =="
"$ADB" devices | grep -q "$BOX" || { echo "box not reachable"; exit 1; }

echo "== installing launcher =="
"$ADB" -s "$BOX:5555" install -r "$(wslpath -w "$APK")" || exit 1

echo "== setting as HOME (best effort) =="
"$ADB" -s "$BOX:5555" shell cmd package set-home-activity labs.nz1.freetv/.MainActivity 2>&1

echo "== free-app inventory on box =="
for p in com.tubitv com.plexapp.android com.mediaworks.android.tv nz.co.dishtv.FreeviewLiveTV com.google.android.youtube.tv org.xbmc.kodi com.android.vending; do
  if "$ADB" -s "$BOX:5555" shell pm list packages | tr -d '\r' | grep -q "^package:$p\$"; then
    echo "  INSTALLED  $p"
  else
    echo "  missing    $p"
  fi
done

echo "== launching NZ1 Free TV =="
"$ADB" -s "$BOX:5555" shell am start -n labs.nz1.freetv/.MainActivity
echo "DONE."
