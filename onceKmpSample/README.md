# onceKmpSample

Sample module that demonstrates how to use [`onceKmp`](../README.md).

## What It Shows

- A reusable wrapper: `OnceKmpSample`
- Platform initialization helpers:
  - Android: `initialiseOnceKmpSample()` or `initialiseOnceKmpSample(context)`
  - Desktop: `initialiseOnceKmpSample()` or `initialiseOnceKmpSample(appUpdatedTimeMillis)`
  - iOS: `initialiseOnceKmpSample()` or `initialiseOnceKmpSample(appUpdatedTimeMillis)`
- Unit tests for common usage flow

## Main Files

- [OnceKmpSample.kt](src/commonMain/kotlin/space/joechen/oncekmpsample/OnceKmpSample.kt)
- [SampleInit.android.kt](src/androidMain/kotlin/space/joechen/oncekmpsample/SampleInit.android.kt)
- [SampleInit.desktop.kt](src/desktopMain/kotlin/space/joechen/oncekmpsample/SampleInit.desktop.kt)
- [SampleInit.ios.kt](src/iosMain/kotlin/space/joechen/oncekmpsample/SampleInit.ios.kt)
