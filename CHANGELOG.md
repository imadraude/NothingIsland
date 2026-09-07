# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-09-07
### Changed & Improved
- **Motion & Fluid Physics**:
  - Replaced desynchronized independent `animateDpAsState` calls with a single unified `updateTransition` in `NothingIslandRoot`, keeping width, height, margins, and corner radius in mathematical harmony.
  - Fine-tuned Apple Fluid spring physics: critically damped settle (`dampingRatio = 1.0f`) on collapse and organic fluid overshoot (`dampingRatio = 0.82f`, `stiffness = Spring.StiffnessMedium`) on expand.
  - Implemented true 1:1 direct manipulation drag gestures with progressive rubber-band damping on vertical and horizontal axes, accompanied by smooth spring settle upon release.
  - Synchronized WindowManager overlay resize lifecycle with Compose spring transitions to eliminate view clipping and flickering when collapsing or returning to Idle.
- **State Engine & Interaction Logic**:
  - Implemented non-destructive priority arbitration in `IslandStateManager`: background battery, notification, or volume HUDs no longer collapse an actively expanded card in the user's face.
  - Refined gesture semantics to match Apple Dynamic Island: short tap on compact pill directly launches the active app (Spotify, Telegram, Settings, Clock), while long press or swipe down smoothly expands the interactive card.
  - Added smart track dismissal suppression: swiping away a playing media track keeps the island dismissed until a new track or artist starts.
  - Added tap-outside-to-collapse support for the overlay service.
- **Nothing OS Visual Polish & Industrial Look**:
  - Redesigned `NdotVisualizer` into an authentic dot-matrix equalizer where individual circular LED dots dynamically illuminate based on waveform amplitudes.
  - Added interactive media scrubber (seek bar) to `ExpandedCardContent` with live dragging time preview and audio transport controls.
  - Refined typography, album art borders, and Nothing Red accents for high OLED contrast.

## [0.2.5] - 2026-09-07
### Fixed
- Fixed critical cutout position bug: `IslandOverlayService` and `IslandApplication.applyLiveCutoutDetection` now strictly preserve factory-calibrated hardware profiles, preventing live WindowInsets from overwriting calibrated coordinates with unadjusted raw cutout values.
- Aligned cutout geometry and pill dimensions with the proven `dynamicSpot` formula:
  - Top margin of camera cutout: exact `12.57dp` (33px).
  - Cutout diameter: exact `22.1dp` (58px).
  - Compact pill height: `38dp` (providing symmetrical `8dp` padding on both top and bottom of the camera punch-hole).
  - Compact pill top margin: `4.57dp` (12px), eliminating visual drooping and perfectly centering the island around the physical front camera.
- Implemented `dynamicSpot` cutout top calculation (`top = bottom - width`) in heuristic detection to bypass AOSP `rect.top == 0` expansion issues.
- Migrated SharedPreferences key to `is_configured_v6` to automatically reload the newly calibrated parameters on existing installations.
- Expanded calibration slider ranges in Settings (Pill Height 20dp-50dp, Compact Width 100dp-240dp).

## [0.2.4] - 2026-09-07
### Changed
- Refined compact pill height to a sleek, thin 26dp (reduced from 32dp), hugging the hardware camera cutout with 1.85dp top & bottom clearance.
- Calibrated camera cutout top margin to 8.5dp for Nothing Phone (2a), preventing the pill from drooping low below the front camera and aligning it seamlessly in the status bar.
- Resized compact media player artwork (20dp), visualizer, and icons (12-15dp) with refined typography (10.5sp) for balanced padding in the 26dp pill.

### Fixed
- Fixed Idle state camera camouflage: animated top margin transition between `cameraTopMarginDp` (in Idle) and `pillTopMarginDp` (in Compact/Expanded), eliminating upward shift and exposing of the camera lens.
- Prioritized factory-calibrated hardware profiles in `CameraCutoutDetector.chooseDetectionResult` to override buggy OEM overlay cutout bounding boxes.
- Extended calibration slider ranges in Settings (Pill Height 20dp-40dp, Camera Diameter 16dp-36dp).
- Migrated SharedPreferences key to `is_configured_v5` to automatically apply the 26dp sleek calibration.

## [0.2.3] - 2026-09-07
### Fixed
- Fixed critical cutout clipping & horizontal displacement on Nothing Phone (2a): bypassed OEM bug in `FrameworksResCommon_Sys_Pacman.apk` (`boundingRectTop` truncated at `X=540` rather than covering the full circle `511..569.5px`), eliminating horizontal misplacement.
- Calibrated hardware cutout parameters via Shizuku (`dumpsys display`):
  - Diameter: exact `22.3dp` (58.5px at 420 dpi, optical sensor `16.3dp`).
  - Vertical center: `23.73dp` (62.3px).
  - Top margin: `12.57dp` (33px).
  - Status bar height: `48.0dp` (126px).
- Symmetrically calibrated compact pill height to `32dp`, achieving exact mathematical symmetry around the camera cutout (`4.86dp` top & bottom clearance) and inside the status bar (`7.72dp` top margin, `8.28dp` bottom margin).
- Expanded dead-center snap threshold to `22dp` in `buildConfigFromRawBounds` and `calculateHeuristicBounds` to prevent OEM bounding-box errors from causing side-to-side jumping.
- Migrated SharedPreferences to `is_configured_v4` to automatically recalibrate and apply the new coordinates for all users.

## [0.2.2] - 2026-09-07
### Fixed
- Fixed critical cutout coordinate displacement in `CameraCutoutDetector`: removed incorrect `viewLocationOnScreen` offset for vector `cutoutPath` which was doubling the screen position and shifting the overlay 166dp off-screen.
- Added permission guard to System Overlay Master Switch in `MainActivity`: prompts user to grant `SYSTEM_ALERT_WINDOW` permission before starting service, preventing crashes.
- Added `DisposableEffect` with `Lifecycle.Event.ON_RESUME` observer in `MainActivity` to automatically refresh permission status upon returning from system settings.
- Linked Master Switch to reactive `IslandOverlayService.isRunning` StateFlow, ensuring switch accurately reflects background service status.
- Added immediate activation feedback: switching the toggle on now displays a spring-animated `"Nothing Island: Active & Calibrated"` pill over the punch-hole.
- Dynamically registered `BatteryStateReceiver` within `IslandOverlayService` lifecycle, restoring battery HUD upon plugging into charger.
- Hardened `IslandOverlayService` with try-catch error boundaries and added `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` for Android 14+ compatibility.

## [0.2.1] - 2026-09-07
### Fixed
- Fixed camera cutout coordinate distortion in floating overlay window by translating window insets with `viewLocationOnScreen`, resolving the horizontal offset bug.
- Added direct physical display cutout extraction via `DisplayManager` (`display.cutout`), eliminating reliance on attached visual window context.
- Refined OLED camera camouflage in `Idle` state: reduced overlay window and pill to exact camera diameter (`28dp`) centered directly above the camera punch-hole.
- Fixed `Expanded` card centering on devices with corner camera punch-holes (e.g. Nothing Phone 1).
- Added `layoutInDisplayCutoutMode = ALWAYS` (API 30+) / `SHORT_EDGES` (API 28-29) in `MainActivity` for uninhibited hardware cutout detection.
- Restored `gradlew.bat` for native Windows development and Gradle builds.

## [0.2.0] - 2026-09-07
### Changed
- Reworked camera cutout detection to prioritize live geometry from the attached overlay window over device-specific profiles.
- Normalized cutout coordinates against the full physical display and refreshed detection after overlay layout and configuration changes.
- Expanded supported punch-hole sizes beyond the previous Nothing-specific 22-36dp range.

### Fixed
- Prevented stale automatically detected geometry from being reused indefinitely across launches.
- Preserved manual calibration when live window insets arrive.
- Fixed the overlay listener using its own narrow window width instead of the full display width.

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
