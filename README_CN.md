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

- Group：`space.joechen`
- Artifact：`oncekmp`
- Kotlin package：`space.joechen`
- Android namespace：`space.joechen`

## 接入方式

### Maven Central（推荐）

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("space.joechen:oncekmp:<latest-version>")
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
  - `java.util.prefs.Preferences.userRoot().node("/oncekmp/<storeName>")`
  - `get()/put()/remove()/clear()/keys()`
- 默认节点根：
  - `/oncekmp`
- 常见后端存储位置：
  - macOS：`~/Library/Preferences/com.apple.java.util.prefs.plist`
  - Linux：`~/.java/.userPrefs/oncekmp/...`
  - Windows：`HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs\\oncekmp\\...`

## 本地开发

查看工程模块：

```bash
./gradlew projects
```

运行库测试：

```bash
./gradlew :onceKmp:allTests --stacktrace
```

如果机器没有 Xcode 工具链，仅跑 JVM/Android：

```bash
./gradlew :onceKmp:desktopTest :onceKmp:testDebugUnitTest
```

发布到本地 Maven：

```bash
./scripts/publish-oncekmp.sh local
```

## 发布到 Maven Central

项目使用 `com.vanniktech.maven.publish`。

必须环境变量：

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
```

可选覆盖参数：

```bash
export ONCEKMP_GROUP=space.joechen
export ONCEKMP_ARTIFACT_ID=oncekmp
export ONCEKMP_VERSION=0.1.0
export ONCEKMP_REPO_URL=https://github.com/<your-org>/<your-repo>
export ONCEKMP_SCM_CONNECTION=scm:git:git://github.com/<your-org>/<your-repo>.git
export ONCEKMP_SCM_DEVELOPER_CONNECTION=scm:git:ssh://git@github.com/<your-org>/<your-repo>.git
export ONCEKMP_SIGN_PUBLICATIONS=true
```

执行发布：

```bash
./scripts/publish-oncekmp.sh
```

## GitHub 开源建议检查项

- [x] Apache-2.0 协议（`LICENSE`）
- [x] 清晰的公共仓库结构
- [x] 根目录中英双语 README
- [x] Maven Central 发布配置
- [x] 示例模块和测试

建议首发前补充：

- GitHub Topics：`kotlin`、`kotlin-multiplatform`、`kmp`、`android`、`ios`、`desktop`
- CI：至少跑 `:onceKmp:allTests`
- 首个 release tag（如 `v0.1.0`）

## klibs.io 收录检查项

按 [klibs.io FAQ](https://klibs.io/faq)，一般满足以下条件后约 24 小时自动收录：

- GitHub 开源可访问
- 至少一个 Maven Central 制品
- 至少一个包含 `kotlin-tooling-metadata.json` 的多平台制品
- POM 中 `url` 或 `scm.url` 指向有效 GitHub 仓库

本仓库配置已满足以上条件。

## 参考

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [jonfinerty/Once](https://github.com/jonfinerty/Once)
- [klibs.io FAQ](https://klibs.io/faq)
