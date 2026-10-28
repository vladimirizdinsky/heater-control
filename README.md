# HeaterControl

Android app for controlling smart heaters on a home network through a self-hosted
**heater-hub** server (Raspberry Pi, REST API). The app talks only to that server, never to
the heaters or the manufacturers' clouds.

Status: early development. Design and specification: [docs/spec.md](docs/spec.md).

## Features (planned for v1)

- Sign in by scanning a pairing QR code (no passwords).
- Device list with quick controls: power, heating, fan speed.
- Device detail generated from what each device reports about itself.
- Pairing other phones from the app.

## Tech

Kotlin, Jetpack Compose, Material 3, Ktor client, kotlinx.serialization.

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17+ (Android Studio's bundled JBR works) and the Android SDK.
