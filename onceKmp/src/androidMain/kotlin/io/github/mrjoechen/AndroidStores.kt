package io.github.mrjoechen

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build

class AndroidSharedPreferencesStoreFactory(
    private val context: Context,
) : OnceStoreFactory {
    override fun createStore(name: String): KeyValueStore {
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        return AndroidKeyValueStore(preferences)
    }
}

fun Once.initialise(context: Context) {
    initialise(
        storeFactory = AndroidSharedPreferencesStoreFactory(context),
        appUpdatedTimeMillis = resolveLastAppUpdateTime(context),
    )
}

fun Once.initialise() {
    val context = OnceAndroidContextHolder.applicationContext
        ?: throw IllegalStateException(
            "Android Context is not available yet. " +
                "Call Once.initialise(context) explicitly or ensure OnceContextProvider is merged in manifest.",
        )
    initialise(context)
}

internal object OnceAndroidContextHolder {
    @Volatile
    var applicationContext: Context? = null
}

class OnceContextProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        OnceAndroidContextHolder.applicationContext = context?.applicationContext
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}

private class AndroidKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {
    override fun allKeys(): Set<String> = preferences.all.keys

    override fun getString(key: String): String? = preferences.all[key] as? String

    override fun getLong(key: String): Long? = when (val value = preferences.all[key]) {
        is Long -> value
        is Int -> value.toLong()
        else -> null
    }

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getStringSet(key: String): Set<String>? {
        val rawValues = preferences.all[key] as? Set<*> ?: return null
        val values = mutableSetOf<String>()
        rawValues.forEach { value ->
            val stringValue = value as? String ?: return@forEach
            values.add(stringValue)
        }
        return values
    }

    override fun putStringSet(key: String, value: Set<String>) {
        preferences.edit().putStringSet(key, value.toSet()).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }
}

@Suppress("DEPRECATION")
private fun resolveLastAppUpdateTime(context: Context): Long {
    val packageManager = context.packageManager
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            ).lastUpdateTime
        } else {
            packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        }
    } catch (_: PackageManager.NameNotFoundException) {
        -1L
    }
}
