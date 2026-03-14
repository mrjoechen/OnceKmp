# onceKmp

[中文 README](README_CN.md)

`onceKmp` is a Kotlin Multiplatform library inspired by [jonfinerty/Once](https://github.com/jonfinerty/Once), used to track one-off actions and rate-limited actions across app install, app version, and app session scopes.

## Modules

- `onceKmp`: publishable KMP library module.
- `onceKmpSample`: sample wrapper module showing real usage.

## Features

- Tag-based tracking: `markDone(tag)`, `beenDone(...)`, `lastDone(tag)`
- Scope-based checks:
  - `Once.THIS_APP_INSTALL`
  - `Once.THIS_APP_VERSION`
  - `Once.THIS_APP_SESSION`
- Time-window checks:
  - `beenDone(OnceTimeUnit, amount, tag)`
  - `beenDone(timeSpanInMillis, tag)`
- Count checks:
  - `Amount.exactly(n)`
  - `Amount.moreThan(n)`
  - `Amount.lessThan(n)`
- To-do workflow:
  - `toDo(...)`
  - `needToDo(tag)`
  - `clearToDo(tag)`

## Coordinates

- Group: `space.joechen`
- Artifact: `oncekmp`
- Kotlin package: `space.joechen`
- Android namespace: `space.joechen`

## Install

### Maven Central (recommended)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("space.joechen:oncekmp:<latest-version>")
        }
    }
}
```

### Local multi-module dependency

`settings.gradle.kts`:

```kotlin
include(":onceKmp")
```

Consumer module:

```kotlin
dependencies {
    implementation(project(":onceKmp"))
}
```

## Initialize

Unified call for Android / iOS / Desktop:

```kotlin
Once.initialise()
```

Android explicit fallback:

```kotlin
Once.initialise(applicationContext)
```

Desktop/iOS custom app update timestamp:

```kotlin
Once.initialise(appUpdatedTimeMillis = 1_700_000_000_000L)
```

Desktop custom storage directory (optional):

```kotlin
Once.initialise(
    storageDir = java.nio.file.Paths.get("/custom/path/for/oncekmp")
)
```

## Usage Example

```kotlin
if (!Once.beenDone(Once.THIS_APP_VERSION, "show_whats_new")) {
    // show what's new
    Once.markDone("show_whats_new")
}
```

## Storage Design (Important)

`Once` uses two persisted stores and one in-memory session list:

- Persisted map store name: `PersistedMapTagLastSeenMap`
  - key: `tag`
  - value: list of done timestamps (`Long`) serialized as comma-separated text in storage adapters.
- Persisted set store name: `PersistedSetToDoSet`
  - key: `PersistedSetValues`
  - value: to-do tag set.
- In-memory only: session tag list for `THIS_APP_SESSION` checks.

## Platform Storage Details

### Android

- Storage API:
  - `Context.getSharedPreferences(name, Context.MODE_PRIVATE)`
  - `SharedPreferences` read/write via `getAll()/putString()/putStringSet()/remove()/clear()`
- App version timestamp API:
  - `PackageManager.getPackageInfo(...).lastUpdateTime`
- Default storage files:
  - `/data/data/<applicationId>/shared_prefs/PersistedMapTagLastSeenMap.xml`
  - `/data/data/<applicationId>/shared_prefs/PersistedSetToDoSet.xml`
- Auto init context capture:
  - `OnceContextProvider` is declared in library manifest and caches `applicationContext`.

### iOS

- Storage API:
  - `NSUserDefaults.standardUserDefaults`
  - `stringForKey/objectForKey/arrayForKey/setObject/removeObjectForKey/dictionaryRepresentation`
- Key namespace rule:
  - `<storeName>:<key>`
  - example: `PersistedMapTagLastSeenMap:show_whats_new`
- Physical storage (Apple managed plist):
  - device: `/var/mobile/Containers/Data/Application/<UUID>/Library/Preferences/<bundle-id>.plist`
  - simulator: `~/Library/Developer/CoreSimulator/Devices/<UDID>/data/Containers/Data/Application/<UUID>/Library/Preferences/<bundle-id>.plist`

### Desktop (JVM)

- Storage API:
  - `java.nio.file` + `java.util.Properties`
  - each store is written as `<storeName>.properties` file
- Default root node (auto-detected):
  - priority: `oncekmp.desktop.appId` system property
  - then: `app.id` / `application.id` / `app.identifier` / `bundle.id` / `app.name`
  - then: runtime identity (`jpackage.app-path` / `sun.java.command` / `java.class.path`)
  - final fallback: `oncekmp-app-<working-directory-name>`
- Default storage directory:
  - macOS: `~/Library/Application Support/<rootNode>/oncekmp/`
  - Linux: `${XDG_CONFIG_HOME:-~/.config}/<rootNode>/oncekmp/`
  - Windows: `%APPDATA%\\<rootNode>\\oncekmp\\`
- Notes:
  - different apps are isolated by different `<rootNode>`
  - uninstalling an app does not always guarantee removal of user config data on all desktop installers
  - deleting the app-specific data directory always clears `oncekmp` state

## References

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [jonfinerty/Once](https://github.com/jonfinerty/Once)
