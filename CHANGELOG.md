# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.9] - 2026-09-07
### Fixed
- Fixed camera cutout peek issue on Nothing Phone (2a): Increased resting pill height to 40dp (with dynamic status bar height adaptation) to provide 6dp of solid black OLED coverage above and below the 28dp camera cutout.
- Added dynamic status bar height detection in `CameraCutoutDetector` (`WindowInsets.Type.statusBars()` with AOSP `dimen.status_bar_height` fallback) ensuring the pill aligns seamlessly with the status bar.
- Migrated shared preferences to `is_configured_v3` so existing devices automatically adopt the corrected dimensions without requiring manual resets.
- Expanded calibration pill height slider range in `MainActivity` from 28-44dp to 28-52dp.

## [0.1.8] - 2026-09-07
### Fixed
- Completely eliminated side-to-side jumping and jitter: removed conflicting WindowInsets listener from floating overlay view (`ComposeView`) which was recalculating horizontal offsets using the window's width instead of full screen width.
- Added factory-calibrated hardware profiles for Nothing Phone (2a) (`A142`/`Pacman`), Nothing Phone (2) (`A065`), and Nothing Phone (1) (`A063`) guaranteeing exact alignment with hardware punch-holes.
- Enforced dead-center snapping (`0f` offset) for all near-center cutouts, preventing subpixel drift.
- Removed reactive slider feedback loop in `MainActivity` to ensure manual adjustments and auto-detect are rock-solid and stable.

## [0.1.7] - 2026-09-07
### Added
- Multi-tier high-precision camera cutout detection system `CameraCutoutDetector`:
  - **Android 12+ (API 31+)**: Exact subpixel vector contour extraction via `DisplayCutout.cutoutPath` and `Path.Op.INTERSECT`.
  - **Android 9-11 (API 28-30)**: AOSP hardware SVG specification parsing (`config_mainBuiltInDisplayCutout`) via `PathParser`.
  - **Intelligent Heuristic Fallback**: Solves AOSP's artificial `rect.top == 0` expansion by centering the punch-hole in status bar bounds.
- Window lifecycle insets listener in `IslandOverlayService` (`setOnApplyWindowInsetsListener`) ensuring live hardware insets after attach.
- Auto-hide Dynamic Island in Landscape orientation to prevent obstruction during media and gaming.
- Comprehensive technical research document in `docs/research/camera_cutout_detection.md`.
- Unit test suite `CameraCutoutDetectorTest` validating bounds calculations and conversion.

### Fixed
- Fixed critical compatibility bug where `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` was used on API 28-29 (now safely falls back to `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`).
- Fixed application context insets query returning `null` or distorted coordinates.
- Preserved user manual calibration overrides when live system insets arrive.

### Changed
- Refined default pill and expanded card dimensions to be much sleeker and proportional to physical Nothing Phone (2a) camera:
  - Resting pill height reduced from 40dp to 34dp (28dp hardware camera + 3dp OLED margins).
  - Compact media pill width reduced from 184dp to 136dp.
  - Expanded card height reduced to 190dp with camera header clearance.
  - Proportional icons, album art, and typography.
- Calibrated slider ranges in MainActivity (pill height 28-44dp, camera diameter 20-40dp, compact width 100-220dp).

## [0.1.5] - 2026-09-07

## [0.1.4] - 2026-09-07

## [0.1.3] - 2026-09-07

## [0.1.2] - 2026-09-07

## [0.1.1] - 2026-09-07

## [0.1.0] - 2026-09-07
### Added
- Initial project architecture tailored for **Nothing Phone (2a)**.
- Deep domain state engine `IslandStateManager` managing states: `Idle`, `Compact`, `Expanded`.
- Physical camouflage using pure OLED black `#000000` to mask centered selfie punch-hole camera.
- `NothingIslandRoot` with Apple-style interruptible spring transitions via Jetpack Compose.
- `NdotVisualizer`: Animated dot-matrix audio equalizer waveform matching Nothing OS design language.
- `IslandOverlayService`: Android Foreground Service rendering floating Compose view using `TYPE_APPLICATION_OVERLAY`.
- `IslandNotificationListener`: Intercepts active media sessions (Spotify, YouTube Music) and incoming notifications.
- `BatteryStateReceiver`: Charging status and battery HUD.
- `MainActivity`: In-app interactive simulator with live calibration sliders for Nothing Phone (2a) cutout dimensions.
- Unit tests suite for `IslandStateManager`.
- GitHub Actions CI/CD workflow for automated testing and APK building.
