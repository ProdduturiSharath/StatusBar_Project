# StatusBar Total

A personal, fully offline Android app that keeps a running total of spending
and dues and shows it all the time as a number in the status bar (in
thousands: `14.5` = ₹14,500). Built for one phone: iQOO Neo 10, OriginOS 6,
Android 16. Sideloaded; no Play Store, no internet, no account.

Permissions: show notifications, start after boot. Nothing else.

## Version history

| Version | What it added |
| --- | --- |
| **V1** | Status bar total drawn as the notification icon; add amounts from the notification's "+ Add" reply box (negative amounts correct mistakes); survives swipe-away, reboot and app updates; history kept on reset. |
| **V2** | Categories (UPI, CC, CC UPI); credit cards with 3-letter codes (`450 abc`); recurring payments (EMI with months left, Autopay, Subscription) added automatically every month; Reset; per-bucket breakdown in the notification. |
| **V3** | New colourful UI (Jetpack Compose, Material 3) with Home, History, Recurring and Cards tabs; donut chart by bucket; history with undo; upcoming dues this month; automatic monthly reset day. |

| **V3.1** | Optimized release build (R8, whole app): about 2.3 MB instead of 26 MB, smoother UI. Signed with the same key, so it updates V3.0 in place. |
| **V4.0** | Delete history entries (one, a cycle, or all; removed from totals); CC requires a card and a default card for CC UPI; edit saved cards (name, bill due day, CC UPI default); monthly budget with progress, one alert per cycle and a status bar toggle (total spent or budget left); card bill reminders 3 days before due; Summary tab comparing cycles; CSV cycle report saved to Downloads/StatusBarTotal at each auto-reset, plus Export now. |

V1 and V2 were not committed separately; this repository starts at V3. The
design documents for every version are in `docs/designs/`.

## Notification "+ Add" quick reference

| Type | Goes to |
| --- | --- |
| `450` or `450 upi` | UPI |
| `450 cc` | rejected: CC needs a card code |
| `450 ccupi` | CC UPI on the default CC UPI card |
| `450 abc` / `450 cc abc` / `450 ccupi abc` | card ABC |
| `-200 ...` | correction (subtracts) |

## Build

Requires Android Studio (bundled JDK) and the Android SDK.

```
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Optimized build (use this one on the phone):

```
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew.bat assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk` (signed with the debug
key on purpose, so it installs over debug builds without losing data). Keep
R8 optimization for the whole app: limiting it to library packages crashed the
app on start.

## Install

Phone: Developer options → USB debugging (and "Install via USB"), connect by
USB, then:

```
adb install -r app/build/outputs/apk/release/app-release.apk
```

Updates keep all data as long as they are signed with the same key. Builds
use Android Studio's debug key at `%USERPROFILE%\.android\debug.keystore`;
back that file up. Losing it means the next build cannot update the app
without uninstalling it, which deletes the data.

## Phone setup (OriginOS)

1. Open the app once and allow notifications.
2. Autostart: on for StatusBar Total.
3. Battery: unrestricted / allow high background power use.
4. Lock the app's card in Recents.
5. Status bar icons: Monochrome.

If OriginOS force-stops the app, the number disappears until the app is
opened again; no code can prevent that, which is what steps 2-4 are for.

## Project layout

- `app/src/main/java/com/sharath/statusbartotal/`: `core/` (pure logic),
  `data/` (ledger, cards, recurring, settings files), `notify/` (status bar
  icon and notification), `receivers/`, `ui/` (Compose screens).
- `docs/designs/`: design documents (V1, V2, V3) and `docs/context/`.
- `review/`, `concepts_brief.md`: design review reports from the planning
  phase.
