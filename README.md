# Music Player (Android)

A full-featured local music player built with Kotlin, Jetpack Compose, Media3
(ExoPlayer + MediaSession), and Room.

## Feature checklist

**Basic**
- Play / Pause, Next / Previous, Shuffle, Repeat (off / all / one), Seek bar, Volume control

**Music**
- Auto-scans the device library via MediaStore
- Supports MP3, FLAC, WAV, AAC, M4A, OGG, OPUS out of the box (all standard
  Android/ExoPlayer formats). ALAC is muxed in M4A/MP4 containers - it will be
  *detected* by the scanner, but whether it *plays* depends on the device's
  platform decoder, since there's no bundled software ALAC decoder. That's why
  it's listed as "device dependent" in the spec.

**UI**
- Album art (via Coil, loaded from MediaStore's embedded art)
- Dark mode (follows system theme automatically, Material You dynamic color on Android 12+)
- Material 3 design system
- Folder browser (songs grouped by containing directory)
- Favorites (persisted in Room)
- Playlists (create, delete, add/remove songs - persisted in Room)
- Search (title / artist / album)

**Advanced**
- Equalizer (system `android.media.audiofx.Equalizer`, attached to the live
  ExoPlayer audio session - band sliders + manufacturer presets)
- Lyrics (optional - if a `.lrc` file with the same name sits next to the
  audio file, it's parsed and shown on the Now Playing screen)
- Sleep timer (pauses playback after N minutes, cancel anytime)
- Lock screen controls (automatic - comes from the MediaSession)
- Notification controls (automatic - comes from Media3's MediaSessionService)
- Background playback (foreground service, survives app close / screen off)
- Android Auto - **not implemented yet**, stubbed as "planned" in Settings, per
  your "later" note. Adding it later mainly means declaring the
  `automotive_app_desc.xml` metadata and it will work off the same MediaSession
  you already have.

## Project structure

```
app/src/main/java/com/example/musicplayer/
  MainActivity.kt            - permission requests, hosts the Compose nav graph
  MusicApplication.kt
  data/                      - Song model, MediaStore scanner, Room DB/DAO/entities
  playback/                  - PlaybackService (MediaSessionService), MusicController,
                               EqualizerController, SleepTimer
  ui/theme/                  - Material 3 theme, typography
  ui/navigation/             - Bottom nav + NavHost wiring
  ui/components/             - SongRow, MiniPlayer
  ui/screens/                - Library, Search, Folders, Favorites, Playlists,
                               NowPlaying, Equalizer, Settings
  viewmodel/                 - MusicViewModel (single source of truth for the UI)
  util/LyricsLoader.kt       - optional local .lrc reader
```

## How to build

1. Open the `MusicPlayerApp` folder in **Android Studio** (Koala/2024.1 or newer).
2. Let Gradle sync - Android Studio will regenerate the Gradle wrapper jar
   automatically on first sync (only `gradle-wrapper.properties` is included
   in this download to keep the zip small).
3. Run on a device or emulator with **Android 7.0 (API 24)** or newer.
4. On first launch, grant the music/notification permission prompt - the
   library scan starts right after.

## Notes / things you may want to tweak

- `applicationId` is `com.example.musicplayer` - change it in
  `app/build.gradle.kts` before publishing anywhere.
- Launcher icons are simple placeholder PNGs - swap
  `app/src/main/res/mipmap-hdpi/ic_launcher*.png` for real artwork (or generate
  a full mipmap set via Android Studio's Image Asset Studio).
- The equalizer only attaches once something has started playing at least
  once (it needs a live ExoPlayer audio session id) - the screen shows a
  short message until then.
- Playlists/Favorites persist across restarts (Room), but the scanned library
  itself is re-scanned from MediaStore every time the app opens - so newly
  added songs on the device show up automatically.

## ACE Media Player TV Edition

This repository is the TV-specific edition of ACE Media Player. It is optimized for Android TV in landscape orientation and uses a separate application ID (`com.example.musicplayer.tv`) so it can coexist with the mobile edition.

### TV controls
- D-pad navigation with visible cyan focus highlight
- OK/Center reveals the video controls when hidden
- D-pad Left/Right navigates the control rows
- OK activates the selected control
- Dedicated -10 sec and +10 sec video controls
- Play/Pause media-key support
- TV-friendly large library/video rows
- Cyan/violet TV splash and launcher artwork

The existing playback engine and media features are reused from the stable ACE 2.x codebase; touch gestures are not required for TV navigation.
