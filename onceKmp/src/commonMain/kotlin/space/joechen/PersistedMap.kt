package space.joechen

internal class PersistedMap(
    private val store: KeyValueStore,
) {
    private val map = mutableMapOf<String, MutableList<Long>>()

    init {
        store.allKeys().forEach { key ->
            val values = store.getString(key)?.let { stringToList(it) } ?: loadFromLegacyStorageFormat(key)
            map[key] = values.toMutableList()
        }
    }

    fun get(tag: String): List<Long> = map[tag]?.toList() ?: emptyList()

    fun put(tag: String, timeSeen: Long) {
        val lastSeenTimestamps = map[tag]?.toMutableList() ?: mutableListOf()
        lastSeenTimestamps.add(timeSeen)
        map[tag] = lastSeenTimestamps
        store.putString(tag, listToString(lastSeenTimestamps))
    }

    fun remove(tag: String) {
        map.remove(tag)
        store.remove(tag)
    }

    fun clear() {
        map.clear()
        store.clear()
    }

    private fun loadFromLegacyStorageFormat(key: String): List<Long> {
        val value = store.getLong(key) ?: return emptyList()
        val values = listOf(value)
        store.putString(key, listToString(values))
        return values
    }

    private fun listToString(values: List<Long>): String = values.joinToString(LIST_DELIMITER)

    private fun stringToList(stringList: String): List<Long> {
        if (stringList.isEmpty()) {
            return emptyList()
        }
        return stringList
            .split(LIST_DELIMITER)
            .mapNotNull { token -> token.toLongOrNull() }
    }

    private companion object {
        private const val LIST_DELIMITER = ","
    }
}
