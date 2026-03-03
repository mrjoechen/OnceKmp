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
  - `/data/user/0/<applicationId>/shared_prefs/PersistedMapTagLastSeenMap.xml`
  - `/data/user/0/<applicationId>/shared_prefs/PersistedSetToDoSet.xml`
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
  - `java.util.prefs.Preferences.userRoot().node("/oncekmp/<storeName>")`
  - `get()/put()/remove()/clear()/keys()`
- Default node root:
  - `/oncekmp`
- Typical backend locations:
  - macOS: `~/Library/Preferences/com.apple.java.util.prefs.plist`
  - Linux: `~/.java/.userPrefs/oncekmp/...`
  - Windows: `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\oncekmp\...`

## Local Development

Build projects:

```bash
./gradlew projects
```

Run library tests:

```bash
./gradlew :onceKmp:allTests --stacktrace
```

If your machine has no Xcode toolchain, run JVM/Android tests only:

```bash
./gradlew :onceKmp:desktopTest :onceKmp:testDebugUnitTest
```

Publish to local Maven:

```bash
./scripts/publish-oncekmp.sh local
```

## Publish To Maven Central

This project uses `com.vanniktech.maven.publish`.

Required env vars:

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
```

Optional publication overrides:

```bash
export ONCEKMP_GROUP=space.joechen
export ONCEKMP_ARTIFACT_ID=oncekmp
export ONCEKMP_VERSION=0.1.0
export ONCEKMP_REPO_URL=https://github.com/<your-org>/<your-repo>
export ONCEKMP_SCM_CONNECTION=scm:git:git://github.com/<your-org>/<your-repo>.git
export ONCEKMP_SCM_DEVELOPER_CONNECTION=scm:git:ssh://git@github.com/<your-org>/<your-repo>.git
export ONCEKMP_SIGN_PUBLICATIONS=true
```

Publish:

```bash
./scripts/publish-oncekmp.sh
```

## GitHub Open Source Checklist

- [x] Apache-2.0 license (`LICENSE`)
- [x] Public source repository structure
- [x] English/Chinese root README
- [x] Maven Central publication config
- [x] Sample module and tests

Recommended before first public release:

- Add GitHub Topics: `kotlin`, `kotlin-multiplatform`, `kmp`, `android`, `ios`, `desktop`.
- Add CI workflow for `:onceKmp:allTests`.
- Create first release tag (for example `v0.1.0`).

## klibs.io Inclusion Checklist

According to [klibs.io FAQ](https://klibs.io/faq), the project is indexed automatically (usually within ~24h) when:

- Source is public on GitHub.
- At least one artifact is published to Maven Central.
- At least one artifact is multiplatform and contains `kotlin-tooling-metadata.json`.
- At least one artifact POM has valid GitHub URL in `url` or `scm.url`.

This repository is configured to satisfy the above conditions.

## References

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [jonfinerty/Once](https://github.com/jonfinerty/Once)
- [klibs.io FAQ](https://klibs.io/faq)
