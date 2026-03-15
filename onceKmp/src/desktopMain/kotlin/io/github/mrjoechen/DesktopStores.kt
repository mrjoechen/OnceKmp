package io.github.mrjoechen

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.util.Base64
import java.util.Properties
import kotlin.time.Duration

private const val STORE_FILE_SUFFIX = ".properties"
private const val STORE_DIR_NAME = "oncekmp"
private const val APP_ID_OVERRIDE_PROPERTY = "oncekmp.desktop.appId"
private const val APP_ID_OVERRIDE_ENV = "ONCEKMP_DESKTOP_APP_ID"
private const val APP_ID_FALLBACK = "oncekmp-app"
private val APP_ID_PROPERTY_CANDIDATES = listOf(
    "app.id",
    "application.id",
    "app.identifier",
    "bundle.id",
    "app.name",
)

class DesktopPreferencesStoreFactory(
    rootNode: String = detectDesktopAppId(),
    private val storageDir: Path = defaultDesktopStorageDirectory(rootNode),
) : OnceStoreFactory {
    override fun createStore(name: String): KeyValueStore {
        return DesktopFileStore(storageDir.resolve("${sanitizeStoreName(name)}$STORE_FILE_SUFFIX"))
    }
}

fun Once.initialise(
    appUpdatedTimeMillis: Long = -1L,
    rootNode: String = detectDesktopAppId(),
    storageDir: Path = defaultDesktopStorageDirectory(rootNode),
) {
    initialise(
        storeFactory = DesktopPreferencesStoreFactory(
            rootNode = rootNode,
            storageDir = storageDir,
        ),
        appUpdatedTimeMillis = appUpdatedTimeMillis,
    )
}

fun defaultDesktopStorageDirectory(rootNode: String = detectDesktopAppId()): Path {
    val appId = sanitizeAppId(rootNode)
    return resolveDesktopConfigRoot().resolve(appId).resolve(STORE_DIR_NAME)
}

internal fun resolveDesktopConfigRoot(
    osName: String = System.getProperty("os.name").orEmpty(),
    homeDir: Path = Paths.get(System.getProperty("user.home", ".")),
    appDataEnv: String? = System.getenv("APPDATA"),
    xdgConfigHomeEnv: String? = System.getenv("XDG_CONFIG_HOME"),
): Path {
    val normalizedOsName = osName.lowercase()
    return when {
        normalizedOsName.contains("mac") -> homeDir.resolve("Library").resolve("Application Support")
        normalizedOsName.contains("win") -> {
            if (!appDataEnv.isNullOrBlank()) {
                Paths.get(appDataEnv)
            } else {
                homeDir.resolve("AppData").resolve("Roaming")
            }
        }
        else -> {
            if (!xdgConfigHomeEnv.isNullOrBlank()) {
                Paths.get(xdgConfigHomeEnv)
            } else {
                homeDir.resolve(".config")
            }
        }
    }
}

internal fun detectDesktopAppId(): String {
    val overrideByProperty = System.getProperty(APP_ID_OVERRIDE_PROPERTY)?.trim().orEmpty()
    if (overrideByProperty.isNotEmpty()) {
        return sanitizeAppId(overrideByProperty)
    }

    val overrideByEnv = System.getenv(APP_ID_OVERRIDE_ENV)?.trim().orEmpty()
    if (overrideByEnv.isNotEmpty()) {
        return sanitizeAppId(overrideByEnv)
    }

    for (propertyName in APP_ID_PROPERTY_CANDIDATES) {
        val candidate = System.getProperty(propertyName)?.trim().orEmpty()
        if (candidate.isNotEmpty()) {
            return sanitizeAppId(candidate)
        }
    }

    val runtimeCandidate = detectDesktopAppIdFromRuntime()
    if (!runtimeCandidate.isNullOrBlank()) {
        return sanitizeAppId(runtimeCandidate)
    }

    return sanitizeAppId(buildDesktopFallbackAppId())
}

fun Once.beenDone(
    timeUnit: java.util.concurrent.TimeUnit,
    amount: Long,
    tag: String,
): Boolean {
    return beenDone(timeUnit.toMillis(amount), tag)
}

fun Once.beenDone(
    timeUnit: java.util.concurrent.TimeUnit,
    amount: Long,
    tag: String,
    numberOfTimes: CountChecker,
): Boolean {
    return beenDone(timeUnit.toMillis(amount), tag, numberOfTimes)
}

fun Once.beenDone(
    duration: Duration,
    tag: String,
    numberOfTimes: CountChecker = Amount.moreThan(0),
): Boolean {
    return beenDone(duration.inWholeMilliseconds, tag, numberOfTimes)
}

private class DesktopFileStore(
    private val filePath: Path,
) : KeyValueStore {
    private val lock = Any()
    private val values = Properties()

    init {
        reloadFromDisk()
    }

    override fun allKeys(): Set<String> = synchronized(lock) {
        reloadFromDisk()
        values.stringPropertyNames().toSet()
    }

    override fun getString(key: String): String? = synchronized(lock) {
        reloadFromDisk()
        values.getProperty(key)
    }

    override fun getLong(key: String): Long? = synchronized(lock) {
        reloadFromDisk()
        values.getProperty(key)?.toLongOrNull()
    }

    override fun putString(key: String, value: String) {
        synchronized(lock) {
            reloadFromDisk()
            values.setProperty(key, value)
            persistToDisk()
        }
    }

    override fun getStringSet(key: String): Set<String>? = synchronized(lock) {
        reloadFromDisk()
        val rawValue = values.getProperty(key) ?: return@synchronized null
        decodeSet(rawValue)
    }

    override fun putStringSet(key: String, value: Set<String>) {
        synchronized(lock) {
            reloadFromDisk()
            values.setProperty(key, encodeSet(value))
            persistToDisk()
        }
    }

    override fun remove(key: String) {
        synchronized(lock) {
            reloadFromDisk()
            values.remove(key)
            persistToDisk()
        }
    }

    override fun clear() {
        synchronized(lock) {
            values.clear()
            Files.deleteIfExists(filePath)
        }
    }

    private fun reloadFromDisk() {
        if (!Files.exists(filePath)) {
            values.clear()
            return
        }

        val reloaded = Properties()
        val loaded = runCatching {
            Files.newInputStream(filePath).use { input ->
                reloaded.load(input)
            }
        }.isSuccess

        if (loaded) {
            values.clear()
            values.putAll(reloaded)
        }
    }

    private fun persistToDisk() {
        val parent = filePath.parent ?: return
        Files.createDirectories(parent)

        if (values.isEmpty) {
            Files.deleteIfExists(filePath)
            return
        }

        val tempFile = Files.createTempFile(parent, "${filePath.fileName}.", ".tmp")
        try {
            Files.newOutputStream(tempFile, CREATE, TRUNCATE_EXISTING, WRITE).use { output ->
                values.store(output, null)
            }
            moveReplace(tempFile, filePath)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    private fun moveReplace(source: Path, target: Path) {
        try {
            Files.move(source, target, REPLACE_EXISTING, ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, REPLACE_EXISTING)
        }
    }

    private companion object {
        private const val SET_DELIMITER = ","

        private fun encodeSet(value: Set<String>): String {
            if (value.isEmpty()) {
                return ""
            }

            return value.joinToString(SET_DELIMITER) { entry ->
                Base64.getEncoder().encodeToString(entry.toByteArray(StandardCharsets.UTF_8))
            }
        }

        private fun decodeSet(rawValue: String): Set<String> {
            if (rawValue.isEmpty()) {
                return emptySet()
            }

            val decodedValues = mutableSetOf<String>()
            rawValue.split(SET_DELIMITER).forEach { encoded ->
                val bytes = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() ?: return@forEach
                decodedValues += String(bytes, StandardCharsets.UTF_8)
            }
            return decodedValues
        }
    }
}

private fun detectDesktopAppIdFromRuntime(): String? {
    val jpackageAppId = detectDesktopAppIdFromJpackage()
    if (!jpackageAppId.isNullOrBlank()) {
        return jpackageAppId
    }

    val commandAppId = detectDesktopAppIdFromCommand()
    if (!commandAppId.isNullOrBlank()) {
        return commandAppId
    }

    val classPathAppId = detectDesktopAppIdFromClassPath()
    if (!classPathAppId.isNullOrBlank()) {
        return classPathAppId
    }

    return null
}

private fun detectDesktopAppIdFromJpackage(): String? {
    val jpackagePath = System.getProperty("jpackage.app-path")?.trim().orEmpty()
    if (jpackagePath.isEmpty()) {
        return null
    }

    val fileName = runCatching { Paths.get(jpackagePath).fileName?.toString() }.getOrNull()?.trim().orEmpty()
    if (fileName.isEmpty()) {
        return null
    }

    return stripExtension(fileName)
}

private fun detectDesktopAppIdFromCommand(): String? {
    val command = System.getProperty("sun.java.command")?.trim().orEmpty()
    if (command.isEmpty()) {
        return null
    }

    val firstToken = command.substringBefore(' ').trim()
    if (firstToken.isEmpty()) {
        return null
    }

    val fileNameLike = firstToken.substringAfterLast('/').substringAfterLast('\\')
    if (fileNameLike.endsWith(".jar", ignoreCase = true)) {
        return stripExtension(fileNameLike)
    }

    if (fileNameLike.contains('.')) {
        return fileNameLike
    }

    val workingDirectoryName = currentWorkingDirectoryName()
    if (!workingDirectoryName.isNullOrBlank()) {
        return "$fileNameLike-$workingDirectoryName"
    }

    return fileNameLike
}

private fun detectDesktopAppIdFromClassPath(): String? {
    val classPath = System.getProperty("java.class.path")?.trim().orEmpty()
    if (classPath.isEmpty()) {
        return null
    }

    val firstClassPathEntry = classPath
        .split(File.pathSeparatorChar)
        .firstOrNull { entry -> entry.isNotBlank() }
        ?: return null

    val fileName = runCatching {
        Paths.get(firstClassPathEntry).fileName?.toString()
    }.getOrNull()?.trim().orEmpty()
    if (fileName.isEmpty()) {
        return null
    }

    return stripExtension(fileName)
}

private fun buildDesktopFallbackAppId(): String {
    val workingDirectoryName = currentWorkingDirectoryName()
    return if (workingDirectoryName.isNullOrBlank()) {
        APP_ID_FALLBACK
    } else {
        "$APP_ID_FALLBACK-$workingDirectoryName"
    }
}

private fun currentWorkingDirectoryName(): String? {
    val userDir = System.getProperty("user.dir")?.trim().orEmpty()
    if (userDir.isEmpty()) {
        return null
    }

    val workingDirectoryName = runCatching {
        Paths.get(userDir).fileName?.toString()
    }.getOrNull()?.trim().orEmpty()

    return workingDirectoryName.ifEmpty { null }
}

private fun stripExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf('.')
    return if (dotIndex <= 0) fileName else fileName.substring(0, dotIndex)
}

private fun sanitizeStoreName(raw: String): String {
    val sanitized = buildString(raw.length) {
        raw.trim().forEach { char ->
            when {
                char.isLetterOrDigit() || char == '.' || char == '_' || char == '-' -> append(char)
                else -> append('-')
            }
        }
    }.trim('-')
    return if (sanitized.isEmpty()) "store" else sanitized
}

private fun sanitizeAppId(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return APP_ID_FALLBACK
    }

    val sanitized = buildString(trimmed.length) {
        var previousDash = false
        trimmed.forEach { char ->
            val normalizedChar = when {
                char.isLetterOrDigit() -> char.lowercaseChar()
                char == '.' || char == '_' || char == '-' -> char
                else -> '-'
            }
            if (normalizedChar == '-' && previousDash) {
                return@forEach
            }
            append(normalizedChar)
            previousDash = normalizedChar == '-'
        }
    }.trim('-', '.', '_')

    return if (sanitized.isEmpty()) APP_ID_FALLBACK else sanitized
}
