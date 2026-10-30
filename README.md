# h96-tv — minimal Android TV launcher for H96-class TV boxes

Kodi-free launcher for cheap H96 Android TV boxes (adb-over-LAN setup): boot into a clean
home screen with the NZ free-to-air apps you need (Freeview, Māori+, ThreeNow, Zank TV).

APKs are not redistributed here (upstream apps, own licenses) — sideload your own into `apks/`.

Build: `./gradlew assembleDebug` (Android SDK; set `local.properties` sdk.dir).
Side-load: `adb connect <box-ip> && adb install -r app/build/outputs/apk/debug/app-debug.apk`
