# Nothing Island (2a) — Design & Architecture Blueprint

## 1. Design Philosophy
- **Physical Camouflage**: The centered punch-hole camera of the Nothing Phone (2a) (~34dp diameter, ~11dp from top) is encapsulated within an OLED true black (`#000000`) container. The camera lens physically becomes the black center of the Dynamic Island.
- **Nothing OS Aesthetics**:
  - Pure OLED black background with subtle `#222222` perimeter definition.
  - Nothing Red (`#D71920`) accent for active playback, notification alerts, and battery status.
  - NDot audio visualizer: dot-matrix animated equalizer bars.
  - Monospace / NDot-inspired technical typography.
- **Apple Fluid Motion via Jetpack Compose**:
  - Built with interruptible springs (`dampingRatio = 0.78f`, `stiffness = Spring.StiffnessMediumLow`).
  - Velocity and gesture hand-off: swipe up or sideways dismisses or collapses without frame hitching.

## 2. Core Modules & Seams (codebase-design)

```
┌─────────────────────────────────────────────────────────────┐
│                    IslandStateManager                       │
│  (Deep Domain Engine: Priority Arbitration, Auto-dismiss)   │
└──────────────┬───────────────────────────────▲──────────────┘
               │ StateFlow<IslandState>        │ postEvent(IslandEvent)
               ▼                               │
┌──────────────────────────────┐ ┌─────────────┴──────────────┐
│       NothingIslandRoot      │ │  IslandNotificationListener│
│ (Compose Springs & Gestures) │ │   & BatteryStateReceiver   │
└──────────────┬───────────────┘ └────────────────────────────┘
               │ Hosted inside
               ▼
┌──────────────────────────────┐
│     IslandOverlayService     │
│ (WindowManager / Surface FGS)│
└──────────────────────────────┘
```

### Module Responsibilities:
1. **`core:model` & `core:IslandStateManager`**:
   - Small interface, deep behavior.
   - Manages state machine: `Idle` ➔ `Compact` ➔ `Expanded`.
   - Handles priority: Phone calls / high alerts > Active user expansion > Active timers > Background Media > Temporary battery/volume HUD > Idle.
2. **`service:IslandOverlayService`**:
   - Android Foreground Service using `TYPE_APPLICATION_OVERLAY`.
   - Wraps `ComposeView` with custom `ServiceLifecycleOwner`.
   - Dynamically resizes `WindowManager.LayoutParams` to avoid obstructing touches outside the island.
3. **`service:IslandNotificationListener`**:
   - Subscribes to `MediaSessionManager` for global playback (Spotify, Apple Music, YouTube Music).
   - Translates UI actions (`play`, `pause`, `skip`, `seek`) to transport controls.
4. **`ui:components`**:
   - `NothingIslandRoot`: Top-level animated morphing container.
   - `CompactPillContent`: Compact pill flanking the selfie camera.
   - `ExpandedCardContent`: Full control card with seekbar, track info, and playback buttons.
   - `NdotVisualizer`: Animated dot-matrix audio wave bars.
