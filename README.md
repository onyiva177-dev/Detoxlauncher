# Detox Launcher

A minimalist, distraction-reducing Android launcher: a plain text list instead
of an icon grid, gesture navigation instead of a busy dock, and screen-time
numbers next to each app so you actually see what's happening.

## How it behaves

- **Home screen** — shows only apps you've pinned (long-press any app to
  pin/unpin). If nothing is pinned yet, it shows everything so your phone
  stays usable.
- **Swipe left** — reveals the full alphabetical list of every installed app.
- **Swipe right** — opens Google News.
- **Swipe up** — opens Chrome with the Google search bar ready.
- **Swipe down** (while viewing all apps) — back to Favorites.
- **Long-press an app** — pin/unpin it as a favorite.
- Each row shows minutes used **today**, pulled from Android's own Usage
  Access API — no separate tracking service, no extra battery drain.
- On first launch it asks to become your **default Home app** and to grant
  **Usage Access**, since both are required for it to function as a launcher.

## Project structure

```
DetoxLauncher/
├── app/src/main/java/com/detox/launcher/
│   ├── MainActivity.kt       – home screen, gestures, permission flow
│   ├── AppInfo.kt            – simple data model
│   ├── AppListAdapter.kt     – renders the text-only list rows
│   ├── PrefsManager.kt       – stores pinned apps
│   └── UsageTracker.kt       – reads today's per-app screen time
├── app/src/main/res/         – layouts, colors, strings
├── app/src/main/AndroidManifest.xml   – HOME/LAUNCHER intent-filter lives here
├── .github/workflows/build-apk.yml    – auto-builds an APK + GitHub Release
└── docs/index.html           – download landing page (for GitHub Pages)
```

## Getting a working APK — two options

### Option A: Let GitHub build it for you (no Android Studio needed)
1. Create a new GitHub repo and push this whole folder to it.
2. GitHub Actions (`.github/workflows/build-apk.yml`) will automatically
   build a debug APK on every push to `main` and publish it as a **Release**
   tagged `latest`, with the file `DetoxLauncher.apk` attached.
3. In `docs/index.html`, replace `YOUR_USERNAME/YOUR_REPO` in the download
   link with your actual repo path.
4. Turn on **GitHub Pages** for the repo: Settings → Pages → Source →
   deploy from branch → `main` / folder `/docs`. Your download page will be
   live at `https://YOUR_USERNAME.github.io/YOUR_REPO/`.

### Option B: Build locally in Android Studio
1. Open the `DetoxLauncher` folder in Android Studio (it will generate the
   Gradle wrapper for you automatically the first time it syncs).
2. Build → Build Bundle(s) / APK(s) → Build APK(s).
3. The APK will be in `app/build/outputs/apk/debug/`.

## Installing on your phone

1. Download the APK (from the Release or by copying it over yourself).
2. Open it — Android will ask to allow installing from this source once.
3. Install, then press the physical/gesture **Home** button. Android will
   ask which launcher to use — pick **Detox Launcher** and tap **Always**.
4. Grant **Usage Access** when prompted (Settings → Apps → Special access →
   Usage access → Detox Launcher → Allow). This is what lets it show
   screen-time minutes; it works without it too, just without that number.

## Notes / things worth knowing

- Minimum Android version: **8.0 (API 26)**.
- To go back to your old launcher: Settings → Apps → Default apps → Home app
  → pick your old one. Detox Launcher does not block that — it's a detox
  tool, not a lock-out tool.
- Nothing here uploads your app list or usage data anywhere — everything
  stays on-device in local SharedPreferences and Android's own usage stats.
- Debug builds are unsigned with a debug key, which is fine for personal
  use/sideloading but not for the Play Store. For Play Store distribution
  you'd need a release keystore and `assembleRelease`.
