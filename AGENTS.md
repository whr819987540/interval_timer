# AGENTS.md

Guidance for coding agents (and humans) working on this repository. Read this first, then `docs/FEATURES.md` and `docs/DESIGN.md`.

## What this is

“间隔计时” — a single-module Android app (Kotlin, Jetpack Compose, Material 3) for repeating countdowns: set a duration, start, get a configurable vibration alert when time is up, reset to start the next round. It must keep working in the background and must let the user reset from the lock screen without unlocking. UI text is Simplified Chinese.

## Build and verify

Requirements: JDK 17, Android SDK platform 35 + build-tools 35.0.0, `local.properties` with `sdk.dir=…` (git-ignored).

```bash
./gradlew assembleDebug          # compile
./gradlew testDebugUnitTest      # JVM unit tests (state machine, keypad entry, formatting) + screenshot rendering
./gradlew verifyPaparazziDebug   # compare screenshots with app/src/test/snapshots/images
./gradlew recordPaparazziDebug   # re-record screenshots after an intended UI change, then look at them
./gradlew lintDebug              # must report "No issues found"
./gradlew assembleRelease        # R8-minified build; run when touching manifest components or reflection
```

Before finishing a change: `assembleDebug testDebugUnitTest verifyPaparazziDebug lintDebug` must all pass. There is no emulator in CI; Paparazzi screenshots are the visual check, so open the PNGs you re-record.

Network goes through a proxy on some machines. Pass proxy settings as `-D` flags on the command line; never commit credentials to `gradle.properties` or any other file.

## Architecture

```
 UI (Compose) ─┐                     ┌─> TimerStore (SharedPreferences, survives process death)
 Notification ─┤                     ├─> TimerAlarmScheduler (AlarmManager.setAlarmClock)
   actions     ├─> TimerController ──┤
 AlarmReceiver ┤   (only writer of   └─> TimerService.start() when a session becomes active
 TimerService ─┘    StateFlow<TimerState>)
                         │
                         └── observed by: TimerService (notification, alert, auto-silence)
                                          ViewModels → screens, AlarmActivity
```

- `timer/TimerState.kt`: `TimerState` (Idle / Running / Paused / Finished) and `TimerMachine`, pure transition functions. All time values are `SystemClock.elapsedRealtime()`.
- `timer/TimerController.kt`: the single entry point for changing state. Each transition persists, (re)arms or cancels the alarm, and starts the service.
- `service/TimerService.kt`: foreground service (`specialUse`). Renders state → notification; starts/stops `Alerter`; schedules COUNT-mode auto-silence; keeps an in-process backup timer.
- `service/TimerNotifications.kt`: two channels (`timer_running` low, `timer_alert` high). Running/paused use notification ID 1, finished uses ID 2 so each new round re-alerts.
- `receiver/TimerActionReceiver.kt`: notification buttons. `receiver/TimerAlarmReceiver.kt`: alarm callback (may cold-start the process).
- `ui/AlarmActivity.kt`: shown over the keyguard via the full-screen intent; closes itself when the state leaves Finished.
- `AppContainer` in `IntervalTimerApp.kt` is a hand-written DI container; get it with `context.appContainer`.

## Invariants — do not break

1. **Only `TimerController` mutates timer state.** UI, receivers and the service call its methods; nobody writes `TimerStore` or schedules alarms directly.
2. **Notification actions must be broadcast `PendingIntent`s** (`TimerActionReceiver.pendingIntent`). Activity intents require unlocking and would break the lock-screen reset feature.
3. **Every `startForegroundService` must be answered by `startForeground`** — `TimerService.onStartCommand` always calls `promote()`. Starting the service from the background is only legal from exempt contexts (visible activity, notification action, exact alarm). `TimerService.start(force = true)` is for foreground callers only.
4. **Lock-screen visibility**: notifications use `VISIBILITY_PUBLIC`; `AlarmActivity` keeps `showWhenLocked`/`turnScreenOn` (manifest + code for API 26).
5. **Alerts use alarm usage** (`USAGE_ALARM`) so vibration/sound work in the background. Channels have no sound/vibration of their own; `Alerter` plays them according to the in-app settings.
6. Elapsed-realtime state is boot-scoped: `TimerStore` discards state from a previous boot. Do not switch to wall-clock time without handling clock changes.
7. Keep `TimerMachine` pure (no Android imports) so it stays unit-testable.

## UI conventions

The visual system comes from the **ui-ux-pro-max** design knowledge base (product type “Timer & Pomodoro”). `docs/DESIGN.md` records the resulting tokens, contrast ratios and checklist. When changing UI:

- Use `MaterialTheme.colorScheme` roles and tokens from `ui/theme/Tokens.kt` (`Spacing`, `Sizes`, `Motion`, `TimerDigits`). No hex colours or ad-hoc dp/sp values in components. If you add or change a colour, re-check contrast (≥ 4.5:1 text, ≥ 3:1 non-text) in both themes and update the table in `docs/DESIGN.md`.
- Touch targets ≥ 48dp; primary actions 64–80dp; keep primary actions in the lower half of the screen.
- Icons are Material Icons vector drawables in `res/drawable/ic_*.xml` (white fill, tinted at use). No emoji, no `material-icons-extended`.
- Screens are split into a stateful `*Route` (ViewModel, permissions, side effects) and a stateless `*Screen` that takes state + callbacks. Screenshot tests call the stateless screens; add a test in `ScreenshotTest` for new screens or states.
- Respect reduced motion via `rememberReducedMotion()`; animate with `graphicsLayer`/alpha, never by changing layout size.
- Every user-visible string goes in `res/values/strings.xml`.
- Support portrait, landscape and large font scales; check them in screenshots.

## Documentation maintenance

Documentation is part of every change:

| When you… | Update |
|---|---|
| add, change or remove a user-visible feature or limitation | `docs/FEATURES.md` (table, section, “最后更新” date/version) |
| change build requirements, commands, permissions or project layout | `README.md` |
| change colours, typography, spacing, motion or accessibility rules | `docs/DESIGN.md` |
| change architecture, invariants, conventions or verification commands | `AGENTS.md` |
| change UI appearance | re-record Paparazzi snapshots (README screenshots come from them) |

Bump `versionCode`/`versionName` in `app/build.gradle.kts` for releases and keep `docs/FEATURES.md` in sync.
