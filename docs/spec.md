# HeaterControl – specification and design

Android client for **heater-hub** (API v1, see the server's `docs/api.md`). Public repository
(portfolio): no server address, tokens or product pictures in the code.

Status: draft, 2026-10-28.

## 1. Scope of v1

| In | Out (later) |
|---|---|
| Pairing by QR code (+ manual fallback) | Login with a password (the server has none) |
| Bottom navigation: Floor plan (placeholder), Devices, Settings | Floor plan itself (server has no rooms yet) |
| Device list with quick controls | Schedules, notifications, admin screens |
| Device detail with all functions, rename | Live updates (server has no SSE; we poll) |
| Command outcome feedback (failed / overridden) | Widgets, Quick Settings tile, Wear |

## 2. How "login" works – QR pairing

The server has no passwords. Access is a bearer token per user and installation. Typing a
43-character token on a phone is impractical, so the app is paired by **scanning a QR code** with a
short-lived pairing code.

### 2.1 Flow

Server side is implemented; the contract is heater-hub `docs/api.md` sections 3 and 5.0.

1. A signed-in user (web UI, or the app's Settings) presses **Pair device** for an existing
   user; an admin can also create a new user. `POST /api/v1/pairings` returns a `code`
   (`K7QM-4XPT`, single use, 10 min), its `uri` and `expiresAt`.
2. The QR code carries `heaterhub://pair?server=<url-encoded base URL>&code=<code>`. The app
   registers the `heaterhub` scheme (intent filter), so scanning with the phone's own camera
   opens the app too.
3. On the new phone the app starts on the **Connect** screen: **Scan QR code**, or type the
   server URL + code.
4. `POST {server}/api/v1/pair {"code", "clientName": "<Build.MODEL>"}` (no auth) → user +
   token. Store server URL + token, go to Main.
5. Errors: `400 PAIRING_CODE_INVALID` → "This code is no longer valid, create a new one";
   `429 TOO_MANY_ATTEMPTS` → "Too many attempts, try again in a minute"; network → "Server not
   reachable (same Wi-Fi / Tailscale?)"; anything else → "Not a heater-hub server".
6. Any later `401` → clear the token, go to Connect with "Access was revoked".
7. Logout (Settings) → clear token, go to Connect.

Storage: Jetpack DataStore; the token encrypted with an AES-GCM key from Android Keystore
(EncryptedSharedPreferences is deprecated). Exclude it from backup (`data_extraction_rules.xml`).

Transport: plain HTTP on the LAN → `network_security_config.xml` with
`cleartextTrafficPermitted="true"` (an app for one private LAN; documented trade-off). With
`tailscale serve` HTTPS works without changes.

Development: `./gradlew runFakeServer` in heater-hub (port 18080); from the emulator use
`http://10.0.2.2:18080`.

## 3. Navigation

```
Connect (scan QR) ──► Main ──────────────────────────────► Device detail (full screen, no bottom bar)
            ├─ Floor plan   (placeholder "Work in progress")
            ├─ Devices      (start tab)
            └─ Settings     (server, user, pair device (admin), logout, version)
```

- Single activity, Compose, **Navigation 3** (the back stack is a plain list you own: one
  entry `Main(tab)`, one entry `DeviceDetail(id)`). Tab switches do not push to the back stack;
  back from a non-start tab goes to Devices, back from Devices exits.

## 4. Data and refresh

- `GET /api/v1/devices` every **5 s while a screen that shows devices is visible**
  (`stateIn(WhileSubscribed(5_000))` + a loop in the repository). No polling in background.
- One `DeviceRepository` owns the list as `StateFlow<DevicesState>`; the detail screen
  observes the same flow filtered by id (no second polling loop, no inconsistent data).
- Pull-to-refresh on the list triggers an immediate fetch.
- Network error while data is already shown → keep the data, show a small "Connection lost"
  banner; the next successful poll removes it. No data yet → full-screen error with Retry.

### 4.1 Commands

`POST /devices/{id}/commands` answers `202` with a pending command; the device confirms later
(usually < 1 s, worst case 3 × 8 s).

- While a command for a device is in flight, the affected control shows the **requested**
  value with a small progress indicator (not a silent optimistic flip).
- The repository follows the command with `GET /commands/{id}` every 1 s until it is not
  `pending`, then refetches devices. (Watching `pendingCommand` on the device is not enough:
  once it disappears, the outcome and `reason` are gone.)
- Outcome → UI:
  - `confirmed` → nothing (the value is simply there).
  - `failed / notConfirmed` → snackbar "Device did not respond".
  - `aborted / overridden` → snackbar "Changed on the device by someone else".
  - `aborted / superseded` → nothing (it was our own newer command).
- Sync errors: `409 DEVICE_OFFLINE`, `409 FUNCTION_UNAVAILABLE`, `400 …` → snackbar, value
  falls back to the server value.
- Sliders / steppers are **debounced** (~700 ms after the last change) so that one gesture is
  one command. Related changes go in one command (e.g. power on + temperature on Model C).

## 5. Mapping the API to the UI

Principle from the API: **devices describe themselves**. The app never branches on `model` or
`type` ("Klarstein", "Dyson"). Everything below is derived from attribute/function ids and the
vocabulary (docs/api.md section 7), so a new model with the same ids works without an update.

- Known ids → Kotlin enums (`AttributeId`, `FunctionId`, `ModeValue`, `FaultCode`); unknown ids
  are dropped when mapping DTO → domain (compatibility rules, section 8 of the API).
- All texts in `strings.xml` keyed by vocabulary id (`mode_comfort`, `attr_child_lock`, …).
  English default + `values-sk`. One file `VocabularyText.kt` maps enum → `@StringRes`.
- Display name: `friendlyName ?: model`.
- Image: `baseUrl + image` loaded with Coil; placeholder icon if `null` or failed.

### 5.1 Quick controls (list item)

Derived per device:

| Chip | Shown when | Value shown | Highlighted when | Tap |
|---|---|---|---|---|
| **Heating** | device has `targetTemperature` | target temperature | `heating == true` (if the attribute exists), else heating mode + power on | see below |
| **Fan** | device has `fanSpeed` | fan speed (1–10) | `fanRunning == true` | `setPower(!power)` |

Heating chip tap:
- Device **without** fan → `setPower(!power)` (Klarstein: heating off = device off). A
  non-heating mode such as Bornholm's `noHeat` is not used for the chip: on a device whose
  only purpose is heating, "heating off" means off. `noHeat` stays available in the detail.
- Device **with** fan → heating is a mode: `setMode(fan)` when heating, `setMode(heat)` when not;
  while power is off → one command `setPower(true)` + `setMode(heat)`.
- "Heating modes" vs "non-heating modes" is a fixed client set from the vocabulary:
  non-heating = `fan`, `noHeat`; everything else heats.

So for Dyson the fan chip is the on/off switch, for Klarstein the heating chip – exactly as
desired, without knowing the brand. If a function is `available: false`, the chip is disabled.

### 5.2 Device detail

Controls are generated from `functions` (with the value from the linked `attribute`), in the
server's order, with a few known ids promoted into a "hero" section.

| Input | Control |
|---|---|
| `bool` | `ListItem` with `Switch` |
| `number` `targetTemperature` | Hero: large value with − / + buttons (`step`, clamped to `min`–`max`) |
| `number` other (`fanSpeed`, `oscillationAngle`) | `Slider` with `steps` from `min/max/step` |
| `number` + `nullable` (`sleepTimer`) | Chips: Off, plus 3–4 presets within the range; remaining time shown |
| `enum`, ≤ 4 options | `SingleChoiceSegmentedButtonRow` |
| `enum`, > 4 options | Exposed dropdown menu |
| unknown input type | not shown |

Read-only attributes (no function points to them: `currentTemperature`, `heating`,
`fanRunning`) form a status row under the image. `available: false` → control disabled with a
supporting text from `unavailableReason` ("Turn the device on first").

## 6. Screens

### 6.1 Device list (Devices tab)

```
┌──────────────────────────────────────────────┐
│ ┌────┐  Obývačka                    ● ⚠      │  ● green = on, red = off; ⚠ fault
│ │img │  21.5 °C · Heat                ⟳      │  current temp · mode; ⟳ command pending
│ └────┘                                       │
│         [🔥 22 °C]   [🌀 4]                  │  quick-control chips (5.1)
└──────────────────────────────────────────────┘
┌──────────────────────────────────────────────┐  offline: everything grey,
│ ┌────┐  Zadná izba                           │  no power dot, no chips,
│ │img │  Offline                              │  not clickable
│ └────┘                                       │
└──────────────────────────────────────────────┘
```

- Card: image 56 dp on the left; name (titleMedium) + power indicator; second line
  `currentTemperature · mode` (or "Offline"); chips at the bottom.
- Power indicator: not only colour – filled dot on, outlined off (+ content description).
- Suggestions for "something more" on the item: **mode** (above), **fault icon** (the API
  requires faults never to be hidden), **pending command indicator**. Optional later: "heating
  right now" as an animated / tinted flame (already in the chip highlight).
- Offline device: `alpha 0.38`, no chips, no click, second line "Offline".
- Sort: server order (stable). Later a per-user order via preferences.
- Empty / error / loading states (4).

### 6.2 Device detail

- Top app bar: back, display name, edit icon → rename dialog.
- Rename dialog: one `OutlinedTextField` prefilled with `friendlyName` (empty = use the model
  name, shown as the placeholder), max 40 chars with a counter, Save / Cancel. Save →
  `PUT /devices/{id}/preferences`; the response is the updated device → put it into the
  repository.
- Content (scrollable): image (≈ 200 dp, centred) → status row → faults card (if any) → hero
  (power switch + target temperature) → other controls (5.2).
- Device goes offline while open → everything disabled, banner "Device is offline". Device
  disappears from the list → back to the list.

### 6.3 Floor plan

Centered illustration + "Floor plan – coming soon". No ViewModel.

### 6.4 Settings

- User name and role (`/me`), server URL, app version, **Log out** (with confirmation).
- **Pair a device** → pick a user (`GET /api/v1/users`); admins can also create one (id,
  name, role) → full-screen QR of `uri` (drawn by the app), the code as text and a countdown
  (`expiresAt`); "New code" when it expires. Screen stays on
  (`FLAG_KEEP_SCREEN_ON`) while the QR is shown.

## 7. Architecture

- Kotlin, Compose + Material 3, single activity, UDF: ViewModel exposes one
  `StateFlow<UiState>`, receives events as function calls.
- **Ktor client** (OkHttp engine) + kotlinx.serialization (`ignoreUnknownKeys = true`), own
  DTOs written from `docs/api.md`.
- **Hilt** for DI, **DataStore** for credentials, **Coil 3** for images, **Navigation 3**.
- Tests: mappers and quick-control rules (pure JVM), ViewModels with a fake repository,
  API client with Ktor `MockEngine` against JSON from `docs/api.md`.

### 7.1 Packages

```
io.github.vladimirizdinsky.heatercontrol
  HeaterControlApp.kt              @HiltAndroidApp
  MainActivity.kt
  data/
    api/        HeaterHubApi, dto/, ApiException (error code + HTTP status), HttpClientFactory
    auth/       CredentialsStore (DataStore + Keystore), SessionRepository (logged in? logout, 401)
    device/     DeviceRepository (polling, commands, rename), DeviceMapper (DTO → domain)
  domain/
    model/      Device, Attribute, DeviceFunction, Input, Fault, Command, vocabulary enums
    QuickControls.kt               rules from 5.1 (pure functions, unit-tested)
  ui/
    navigation/ routes, AppNavigation
    connect/    ConnectScreen, ConnectViewModel
    main/       MainScaffold (bottom bar)
    devices/    DeviceListScreen, DeviceListViewModel, DeviceCard, QuickControlChip
    detail/     DeviceDetailScreen, DeviceDetailViewModel, RenameDialog, controls/
    floorplan/  FloorPlanScreen
    settings/   SettingsScreen, SettingsViewModel
    common/     VocabularyText, DeviceImage, error/snackbar helpers
    theme/
```

### 7.2 Domain model (sketch)

```kotlin
data class Device(
    val id: String,
    val displayName: String,          // friendlyName ?: model
    val friendlyName: String?,
    val imageUrl: String?,            // already absolute
    val online: Boolean,
    val attributes: Map<AttributeId, AttributeValue>,
    val functions: Map<FunctionId, DeviceFunction>,  // keeps server order (LinkedHashMap)
    val faults: List<Fault>?,
    val pendingCommandId: String?,
)

sealed interface AttributeValue {
    data class Bool(val value: Boolean?) : AttributeValue
    data class Number(val value: Double?, val unit: Unit?) : AttributeValue
    data class Enum(val value: ModeValue?, val raw: String?, val options: List<ModeValue>) : AttributeValue
}
```

## 8. Milestones

1. Dependencies, DTOs, `HeaterHubApi`, mapper + tests (against fake server JSON).
2. Credentials store, session, Connect screen (manual code first, then QR scan + deep link), `GET /me`.
3. Main scaffold with bottom bar, Floor plan + Settings placeholders, logout.
4. Device repository with polling, device list (read-only).
5. Commands + quick controls on the list, snackbar outcomes.
6. Device detail: generated controls, rename.
7. Polish: Slovak strings, dark theme, accessibility, error states, README with screenshots.

## 9. Decisions

- Third tab: Settings. (2026-10-28)
- Fan chip shows fan speed. (2026-10-28)
- Heating chip on a device without fan toggles power, also on Bornholm with `noHeat`. (2026-10-28)
- Login by QR pairing code (implemented on the server); any user can pair from the app, admins can also create users. (2026-10-28)
