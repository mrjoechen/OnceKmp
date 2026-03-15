package io.github.mrjoechen

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

class IosUserDefaultsStoreFactory(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : OnceStoreFactory {
    override fun createStore(name: String): KeyValueStore {
        return IosUserDefaultsStore(userDefaults = userDefaults, storeName = name)
    }
}

fun Once.initialise(
    appUpdatedTimeMillis: Long = -1L,
    userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    initialise(
        storeFactory = IosUserDefaultsStoreFactory(userDefaults),
        appUpdatedTimeMillis = appUpdatedTimeMillis,
    )
}

private class IosUserDefaultsStore(
    private val userDefaults: NSUserDefaults,
    private val storeName: String,
) : KeyValueStore {

    override fun allKeys(): Set<String> {
        val values = userDefaults.dictionaryRepresentation()
        val prefix = prefix()
        val keys = mutableSetOf<String>()
        values.keys.forEach { rawKey ->
            val key = rawKey as? String ?: return@forEach
            if (key.startsWith(prefix)) {
                keys += key.removePrefix(prefix)
            }
        }
        return keys
    }

    override fun getString(key: String): String? = userDefaults.stringForKey(namespacedKey(key))

    override fun getLong(key: String): Long? {
        val value = userDefaults.objectForKey(namespacedKey(key)) as? NSNumber ?: return null
        return value.longLongValue
    }

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = namespacedKey(key))
    }

    override fun getStringSet(key: String): Set<String>? {
        val rawValues = userDefaults.arrayForKey(namespacedKey(key)) ?: return null
        val values = mutableSetOf<String>()
        rawValues.forEach { value ->
            val stringValue = value as? String ?: return@forEach
            values += stringValue
        }
        return values
    }

    override fun putStringSet(key: String, value: Set<String>) {
        userDefaults.setObject(value.toList(), forKey = namespacedKey(key))
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(namespacedKey(key))
    }

    override fun clear() {
        allKeys().forEach { key ->
            remove(key)
        }
    }

    private fun namespacedKey(key: String): String = prefix() + key

    private fun prefix(): String = "$storeName:"
}
