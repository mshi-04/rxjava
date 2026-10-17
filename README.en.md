# Tokyo Weather — RxJava Android Sample

[日本語](README.md) | English

An Android learning sample that displays Tokyo's seven-day forecast from the public 7Timer! API.
The implementation, design documents, and commit history preserve an example of organizing
asynchronous work with RxJava 3, Clean Architecture, and MVVM.
App distribution and operating a hosted service are outside the project's scope.

## Features

- Displays a forecast list on one screen, with the date, weather, low and high temperatures, and wind speed.
- Represents loading, success, and failure as separate UI states, with a retry action after failure.
- Persists results in Room and reuses forecasts fetched less than 24 hours ago.
  Opening the screen does not make a network request when the cache is valid.
- Manages subscriptions in a ViewModel and handles screen rotation and Fragment view recreation.

## Structure

The project has one `app` module, using Java 17, Views defined in XML, and ViewDataBinding.
It uses RxJava 3 / RxAndroid for asynchronous work, Retrofit / Moshi for networking,
Room for persistence, and Hilt for dependency injection.

Dependencies follow `presentation -> domain <- data`; the presentation layer uses MVVM.

| Package | Responsibility |
| --- | --- |
| `domain/` | Android-independent models, value objects, use cases, and repository interfaces |
| `data/` | API requests, DTO mapping, Room persistence, and the 24-hour cache policy |
| `presentation/` | Activity, Fragment, ViewModel, Adapter, and DataBinding rendering |
| `di/` | Hilt dependency assembly and implementation selection |

Source code is under `app/src/main/java/com/example/rxjava/`.
Dependency versions are centralized in the [version catalog](gradle/libs.versions.toml).

## Requirements

| Item | Configuration |
| --- | --- |
| Android Studio | A version compatible with this project's Android Gradle Plugin 9.0.1 |
| Java | Java 17 source and target compatibility; use a compatible Gradle JDK, such as Android Studio's bundled JBR |
| Android SDK | SDK Platform 36 (`compileSdk` / `targetSdk` are 36) |
| Device | An emulator or physical device running Android 7.0 (API 24) or later |
| Gradle | Use the included Wrapper (9.1.0); no separate Gradle installation is needed |

An internet connection is needed for the initial dependency download and for fetching weather
when the cache is absent or expired. No API key or account configuration is required.

## Getting started

1. Open the repository root in Android Studio.
2. Configure SDK Platform 36 and the Gradle JDK, then sync the project.
   Set the SDK location through Android Studio or `sdk.dir` in your local `local.properties`.
   Do not commit `local.properties`.
3. Start an emulator or connect a physical device with debugging enabled.
4. Select the `app` run configuration and use Android Studio's Run action.

To build a debug APK from the command line, use the Wrapper at the repository root.

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`.
The commands in this README were checked against the code and existing documentation during
this documentation update; they were not executed.

## Verification commands

These checks are available for readers to run. In Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`.

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

JVM tests include architecture dependency checks with ArchUnit.
Device tests require an emulator or physical device and cover UI states and the actual Room database.
See the detailed documents below for the test strategy and existing validation records.

## API and known limitations

The app requests 7Timer!'s `civillight` product at a fixed Tokyo location
(latitude 35.690, longitude 139.692). It sends no credentials.
Consult the [official 7Timer! Wiki](https://github.com/Yeqzids/7timer-issues/wiki/Wiki) for the API specification and terms of use.

- There is no location picker, and the app's UI is in Japanese.
- Cache freshness is checked only on the initial load and retries after failure. There is no forced refresh after success or automatic refresh on resume or in the background.
- Room data survives process termination. Force-stopping and reopening the app still shows the same forecast while it is less than 24 hours old.
  If the cache has expired and a network request fails, the app does not fall back to the stale forecast.
- Weather retrieval depends on the external API's availability.
- Release distribution and Room migrations are outside the project's scope. During development, schema changes require clearing the app's data before starting it again.

## Detailed documentation

The detailed documents are in Japanese.

| Document | Contents |
| --- | --- |
| [Architecture (Japanese)](docs/architecture.md) | Layers, dependency direction, asynchronous work, and cache design |
| [Domain (Japanese)](docs/domain.md) | Models, value objects, use cases, and repository contracts |
| [Data (Japanese)](docs/data.md) | Networking, DTOs, data sources, Room, and repository implementation |
| [Presentation (Japanese)](docs/presentation.md) | ViewModel, UI states, DataBinding, and subscription management |
| [Development (Japanese)](docs/development.md) | Environment, builds, external API, existing validation records, and limitations |
| [JVM tests (Japanese)](docs/unit-test.md) | Unit tests and architecture dependency checks |
| [UI and database tests (Japanese)](docs/ui-test.md) | Device test strategy and procedures |
