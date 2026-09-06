# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
