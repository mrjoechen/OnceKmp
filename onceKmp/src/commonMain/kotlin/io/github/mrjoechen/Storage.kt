package io.github.mrjoechen

interface OnceStoreFactory {
    fun createStore(name: String): KeyValueStore
}

interface KeyValueStore {
    fun allKeys(): Set<String>
    fun getString(key: String): String?
    fun getLong(key: String): Long?
    fun putString(key: String, value: String)
    fun getStringSet(key: String): Set<String>?
    fun putStringSet(key: String, value: Set<String>)
    fun remove(key: String)
    fun clear()
}

class InMemoryStoreFactory : OnceStoreFactory {
    private val stores = mutableMapOf<String, InMemoryKeyValueStore>()

    override fun createStore(name: String): KeyValueStore {
        return stores.getOrPut(name) { InMemoryKeyValueStore() }
    }
}

private class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()

    override fun allKeys(): Set<String> = values.keys.toSet()

    override fun getString(key: String): String? = values[key] as? String

    override fun getLong(key: String): Long? = when (val value = values[key]) {
        is Long -> value
        is Int -> value.toLong()
        else -> null
    }

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getStringSet(key: String): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return (values[key] as? Set<String>)?.toSet()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        values[key] = value.toSet()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun clear() {
        values.clear()
    }
}
