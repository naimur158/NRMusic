# NR Music 🎵

A personal, ad-free, Spotify-style music app for Android.

- **Home** — music-only trending songs + genre shortcuts, one-tap Shuffle
- **Search** — the entire YouTube Music catalog (songs from all over the world)
- **Library** — Liked Songs, Downloads, your playlists, device files, and an in-library search box to find anything you've saved
- **Playlists** — create, rename, delete, add/remove, **drag-to-reorder** (long-press a song), custom cover images
- **Offline downloads** — save any song to the phone and play it with no connection
- **Liked Songs** — one-tap favourites, kept in a smart collection
- Player extras — **shuffle**, **repeat (off/all/one)**, **sleep timer**, **queue view**
- Full-screen player, mini player, background playback, lock-screen controls
- Dark Spotify-style UI, zero ads

### More features
- **Equalizer + bass boost**, **playback speed** (0.5×–2×), **sleep timer**
- **Synced lyrics** (via lrclib.net) that scroll with the song
- **Color-adaptive player** that tints to the album art; **swipe artwork** to change track
- **Audio visualizer**
- **Recently played**, **listening stats**, and **smart playlists** (Most played / Recently added)
- **"Because you liked…" recommendations** on Home
- **Artist & album pages**, **genre/mood browse**, **search filters** (songs/videos/albums/artists) + **history & suggestions**
- **Multi-select** (batch add/download), **swipe-to-delete**, **sort** (title/artist/duration)
- **Storage manager**, **Settings** (themes incl. AMOLED/light + accent color), **headset double-tap = next**, **haptics**
- **Android Auto** browse tree, **first-run onboarding**, **pull-to-refresh**, **offline banner**, **auto-retry** on stream failures
- **Signed release APK** + in-app update check (point it at a GitHub repo to enable)

### Automatic updates over GitHub
Push a change → GitHub Actions builds & publishes a signed APK → installed copies
show an "Update available" dialog and update themselves. See **[GITHUB_SETUP.md](GITHUB_SETUP.md)**
for the one-time setup (create repo, add 4 signing secrets, push).

### Building a signed release
`.\gradlew.bat assembleRelease` produces `app/build/outputs/apk/release/app-release.apk`,
signed with `nrmusic-release.jks` (keystore/passwords in `app/build.gradle.kts`).
Keep the `.jks` safe and out of git — you need the same key to ship updates.

### Where your data lives
Playlists, likes, and download records are stored as `library.json` in the app's
private storage; downloaded audio goes to `files/downloads/`. Nothing leaves the
phone and there is no account or server.

## ⚠️ Important

YouTube streaming uses YouTube's internal API via
[NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor), the same
engine used by NewPipe and InnerTune. This violates YouTube's Terms of Service,
so this app is for **personal use / sideloading only** — never upload it to the
Google Play Store.

## Building

1. Install **Android Studio** (this repo was set up with 2026.1.x).
2. Open Android Studio → **Open** → select this `NRMusic` folder.
3. Let the first-run wizard install the Android SDK if prompted, then wait for
   Gradle sync to finish (first sync downloads dependencies, takes a few minutes).
4. Plug in your phone with **USB debugging** enabled (Settings → About phone →
   tap "Build number" 7 times → Developer options → USB debugging), or create an
   emulator via Device Manager.
5. Press **Run ▶**.

To build an installable APK instead: **Build → Build App Bundles/APK(s) → Build APK(s)**.
The APK lands in `app/build/outputs/apk/debug/app-debug.apk` — copy it to your
phone and open it to install.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Media3 / ExoPlayer with `MediaSessionService` for background playback
- NewPipe Extractor for YouTube search + stream resolution
- Coil for artwork loading

## How YouTube playback works

Tracks are queued with a fake `nrmusic://youtube/{videoId}` URI. When ExoPlayer
actually loads a track, a `ResolvingDataSource` calls NewPipe Extractor to fetch
the real (short-lived) audio stream URL. This keeps queues valid indefinitely
even though Google's stream URLs expire after a few hours.

## Known limitations (v1)

- No playlists, downloads/offline caching, or recommendations yet
- Home "Trending" is YouTube's general trending feed, so it can include non-music
- If YouTube changes their internals, bump the NewPipeExtractor version in
  `app/build.gradle.kts` — that usually fixes extraction breakage
