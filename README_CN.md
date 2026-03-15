# OnceKmp

[English README](README.md)

`onceKmp` 是一个 Kotlin Multiplatform 库，灵感来自 [jonfinerty/Once](https://github.com/jonfinerty/Once)，用于管理一次性行为与限频行为，支持安装周期、版本周期、会话周期判断。

## 模块说明

- `onceKmp`：可发布到 Maven 的核心 KMP 库。
- `onceKmpSample`：示例封装模块，展示跨平台使用方式。

## 功能

- 基于 `tag` 的行为记录：`markDone(tag)`、`beenDone(...)`、`lastDone(tag)`
- Scope 判断：
  - `Once.THIS_APP_INSTALL`
  - `Once.THIS_APP_VERSION`
  - `Once.THIS_APP_SESSION`
- 时间窗口判断：
  - `beenDone(OnceTimeUnit, amount, tag)`
  - `beenDone(timeSpanInMillis, tag)`
- 次数判断：
  - `Amount.exactly(n)`
  - `Amount.moreThan(n)`
  - `Amount.lessThan(n)`
- ToDo 流程：
  - `toDo(...)`
  - `needToDo(tag)`
  - `clearToDo(tag)`

## 坐标信息

- Group：`io.github.mrjoechen`
- Artifact：`oncekmp`
- Kotlin package：`io.github.mrjoechen`
- Android namespace：`io.github.mrjoechen`

## 接入方式

### Maven Central（推荐）

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.mrjoechen:oncekmp:<latest-version>")
        }
    }
}
```

### 本地多模块依赖

`settings.gradle.kts`：

```kotlin
include(":onceKmp")
```

使用方模块：

```kotlin
dependencies {
    implementation(project(":onceKmp"))
}
```

## 初始化方式

三端统一调用：

```kotlin
Once.initialise()
```

Android 显式初始化兜底：

```kotlin
Once.initialise(applicationContext)
```

Desktop / iOS 自定义更新时间戳：

```kotlin
Once.initialise(appUpdatedTimeMillis = 1_700_000_000_000L)
```

Desktop 自定义存储目录（可选）：

```kotlin
Once.initialise(
    storageDir = java.nio.file.Paths.get("/custom/path/for/oncekmp")
)
```

## 使用示例

```kotlin
if (!Once.beenDone(Once.THIS_APP_VERSION, "show_whats_new")) {
    // 展示更新说明
    Once.markDone("show_whats_new")
}
```

## 存储模型（重点）

`Once` 内部使用两类持久化存储 + 一类内存会话数据：

- 持久化 Map store：`PersistedMapTagLastSeenMap`
  - key：`tag`
  - value：该 tag 的完成时间戳列表（`Long`），在各平台适配层序列化保存。
- 持久化 Set store：`PersistedSetToDoSet`
  - key：`PersistedSetValues`
  - value：待办 tag 集合。
- 仅内存：session tag 列表（用于 `THIS_APP_SESSION` 统计）。

## 三端存储路径与平台 API 细节

### Android

- 调用的原生存储 API：
  - `Context.getSharedPreferences(name, Context.MODE_PRIVATE)`
  - `SharedPreferences` 的 `getAll()/putString()/putStringSet()/remove()/clear()`
- 版本更新时间 API：
  - `PackageManager.getPackageInfo(...).lastUpdateTime`
- 默认存储文件：
  - `/data/user/0/<applicationId>/shared_prefs/PersistedMapTagLastSeenMap.xml`
  - `/data/user/0/<applicationId>/shared_prefs/PersistedSetToDoSet.xml`
- 自动上下文注入：
  - 库内 `OnceContextProvider` 会在应用启动早期缓存 `applicationContext`，支持 `Once.initialise()` 无参调用。

### iOS

- 调用的原生存储 API：
  - `NSUserDefaults.standardUserDefaults`
  - `stringForKey/objectForKey/arrayForKey/setObject/removeObjectForKey/dictionaryRepresentation`
- key 命名规则：
  - `<storeName>:<key>`
  - 例如：`PersistedMapTagLastSeenMap:show_whats_new`
- 物理存储位置（Apple 管理 plist）：
  - 真机：`/var/mobile/Containers/Data/Application/<UUID>/Library/Preferences/<bundle-id>.plist`
  - 模拟器：`~/Library/Developer/CoreSimulator/Devices/<UDID>/data/Containers/Data/Application/<UUID>/Library/Preferences/<bundle-id>.plist`

### Desktop (JVM)

- 调用的原生存储 API：
  - `java.nio.file` + `java.util.Properties`
  - 每个 store 写入为 `<storeName>.properties` 文件
- 默认 rootNode（自动感知）：
  - 优先级：`oncekmp.desktop.appId` 系统属性
  - 然后：`app.id` / `application.id` / `app.identifier` / `bundle.id` / `app.name`
  - 然后：运行时标识（`jpackage.app-path` / `sun.java.command` / `java.class.path`）
  - 最终回退：`oncekmp-app-<当前工作目录名>`
- 默认存储目录：
  - macOS：`~/Library/Application Support/<rootNode>/oncekmp/`
  - Linux：`${XDG_CONFIG_HOME:-~/.config}/<rootNode>/oncekmp/`
  - Windows：`%APPDATA%\\<rootNode>\\oncekmp\\`
- 说明：
  - 不同应用通过不同 `<rootNode>` 隔离
  - Desktop 端是否在卸载时自动清理用户配置目录，取决于具体安装器与卸载脚本，不能一概保证
  - 删除对应应用数据目录一定可以清空 `oncekmp` 状态

## 参考

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [jonfinerty/Once](https://github.com/jonfinerty/Once)
