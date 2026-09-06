# Nothing Island (2a)

An ultra-fluid, native Android Dynamic Island crafted specifically for the **Nothing Phone (2a)** aesthetic and hardware geometry.

![Nothing Phone 2a](https://img.shields.io/badge/Nothing%20OS-Aesthetic-red)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.11-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-purple)

## Features
- **Seamless Cutout Camouflage**: Calibrated for the centered punch-hole camera of the Nothing Phone (2a).
- **Nothing OS Visual Language**: Pure OLED black (`#000000`), Nothing Red (`#D71920`), and NDot-style animated dot-matrix waveform equalizer.
- **Apple Fluid Motion Physics**: Natural, interruptible spring animations with soft overshoot (`dampingRatio = 0.78f`).
- **Interactive Media Player**: Spotify, YouTube Music, Apple Music playback with live album art and transport controls.
- **Charging & Battery HUD**: Displays battery percentage and fast charging indicator upon plug-in.
- **Interactive In-App Simulator**: Live preview inside `MainActivity` with real-time calibration sliders.

## Building & Running
- **Unit Tests**: Run `./gradlew testReleaseUnitTest`
- **Build APK**: Run `./gradlew assembleRelease` or push to GitHub to build automatically via GitHub Actions (`.github/workflows/build.yml`).
