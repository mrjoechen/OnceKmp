package space.joechen

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences
import kotlin.time.Duration

class DesktopPreferencesStoreFactory(
    private val rootNode: String = "/oncekmp",
) : OnceStoreFactory {
    override fun createStore(name: String): KeyValueStore {
        return DesktopPreferencesStore(Preferences.userRoot().node("$rootNode/$name"))
    }
}

fun Once.initialise(
    appUpdatedTimeMillis: Long = -1L,
    rootNode: String = "/oncekmp",
) {
    initialise(
        storeFactory = DesktopPreferencesStoreFactory(rootNode),
        appUpdatedTimeMillis = appUpdatedTimeMillis,
    )
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

private class DesktopPreferencesStore(
    private val preferences: Preferences,
) : KeyValueStore {
    override fun allKeys(): Set<String> {
        return try {
            preferences.keys().toSet()
        } catch (_: BackingStoreException) {
            emptySet()
        }
    }

    override fun getString(key: String): String? = preferences.get(key, null)

    override fun getLong(key: String): Long? {
        val rawValue = preferences.get(key, null) ?: return null
        return rawValue.toLongOrNull()
    }

    override fun putString(key: String, value: String) {
        preferences.put(key, value)
    }

    override fun getStringSet(key: String): Set<String>? {
        val rawValue = preferences.get(key, null) ?: return null
        if (rawValue.isEmpty()) {
            return emptySet()
        }

        return rawValue
            .split(SET_DELIMITER)
            .map { encoded ->
                val decoded = Base64.getDecoder().decode(encoded)
                String(decoded, StandardCharsets.UTF_8)
            }
            .toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        if (value.isEmpty()) {
            preferences.put(key, "")
            return
        }

        val encoded = value.joinToString(SET_DELIMITER) { entry ->
            Base64.getEncoder().encodeToString(entry.toByteArray(StandardCharsets.UTF_8))
        }
        preferences.put(key, encoded)
    }

    override fun remove(key: String) {
        preferences.remove(key)
    }

    override fun clear() {
        try {
            preferences.clear()
        } catch (_: BackingStoreException) {
        }
    }

    private companion object {
        private const val SET_DELIMITER = ","
    }
}
