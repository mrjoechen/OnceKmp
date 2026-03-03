package space.joechen

internal class PersistedSet(
    private val store: KeyValueStore,
) {
    private val values = (store.getStringSet(STRING_SET_KEY) ?: emptySet()).toMutableSet()

    fun put(tag: String) {
        values.add(tag)
        updateStore()
    }

    fun contains(tag: String): Boolean = values.contains(tag)

    fun remove(tag: String) {
        values.remove(tag)
        updateStore()
    }

    fun clear() {
        values.clear()
        updateStore()
    }

    private fun updateStore() {
        store.putStringSet(STRING_SET_KEY, values)
    }

    private companion object {
        private const val STRING_SET_KEY = "PersistedSetValues"
    }
}
