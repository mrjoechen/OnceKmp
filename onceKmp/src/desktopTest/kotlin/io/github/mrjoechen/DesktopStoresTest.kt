package io.github.mrjoechen

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopStoresTest {

    @Test
    fun appIdOverridePropertyTakesEffect() {
        val original = System.getProperty("oncekmp.desktop.appId")
        try {
            System.setProperty("oncekmp.desktop.appId", "com.example.desktop-app")
            assertEquals("com.example.desktop-app", detectDesktopAppId())
        } finally {
            if (original == null) {
                System.clearProperty("oncekmp.desktop.appId")
            } else {
                System.setProperty("oncekmp.desktop.appId", original)
            }
        }
    }

    @Test
    fun differentRootNodesUseDifferentDirectories() {
        val dirA = defaultDesktopStorageDirectory("app-a")
        val dirB = defaultDesktopStorageDirectory("app-b")

        assertNotEquals(dirA, dirB)
        assertTrue(dirA.toString().contains("app-a"))
        assertTrue(dirB.toString().contains("app-b"))
    }

    @Test
    fun linuxConfigRootRespectsXdgConfigConvention() {
        val home = Paths.get("/home/demo")
        val xdgRoot = resolveDesktopConfigRoot(
            osName = "Linux",
            homeDir = home,
            appDataEnv = null,
            xdgConfigHomeEnv = "/tmp/demo-xdg-config",
        )
        val fallbackRoot = resolveDesktopConfigRoot(
            osName = "Linux",
            homeDir = home,
            appDataEnv = null,
            xdgConfigHomeEnv = null,
        )

        assertEquals(Paths.get("/tmp/demo-xdg-config"), xdgRoot)
        assertEquals(home.resolve(".config"), fallbackRoot)
    }

    @Test
    fun appIdFallbackIsScopedByWorkingDirectory() {
        withTemporaryProperties(
            mapOf(
                "oncekmp.desktop.appId" to "",
                "app.id" to "",
                "application.id" to "",
                "app.identifier" to "",
                "bundle.id" to "",
                "app.name" to "",
                "jpackage.app-path" to "",
                "sun.java.command" to "",
                "java.class.path" to "",
                "user.dir" to "/tmp/OnceKmpDesktopTest",
            ),
        ) {
            assertEquals("oncekmp-app-oncekmpdesktoptest", detectDesktopAppId())
        }
    }

    @Test
    fun storesWithDifferentDirectoriesAreIsolated() {
        val tempDir = Files.createTempDirectory("oncekmp-desktop-store-test")
        try {
            val factoryA = DesktopPreferencesStoreFactory(
                rootNode = "app-A",
                storageDir = tempDir.resolve("app-A"),
            )
            val factoryB = DesktopPreferencesStoreFactory(
                rootNode = "app-B",
                storageDir = tempDir.resolve("app-B"),
            )

            val storeA = factoryA.createStore("PersistedMapTagLastSeenMap")
            val storeB = factoryB.createStore("PersistedMapTagLastSeenMap")

            storeA.putString("sameTag", "1000")

            assertEquals("1000", storeA.getString("sameTag"))
            assertNull(storeB.getString("sameTag"))

            storeB.putString("sameTag", "2000")
            assertEquals("1000", storeA.getString("sameTag"))
            assertEquals("2000", storeB.getString("sameTag"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun clearRemovesPersistedState() {
        val tempDir = Files.createTempDirectory("oncekmp-desktop-clear-test")
        try {
            val store = DesktopPreferencesStoreFactory(
                rootNode = "clear-test",
                storageDir = tempDir.resolve("clear-test"),
            ).createStore("PersistedSetToDoSet")

            store.putStringSet("PersistedSetValues", setOf("a", "b"))
            assertTrue(store.getStringSet("PersistedSetValues")?.isNotEmpty() == true)

            store.clear()

            assertFalse(store.allKeys().isNotEmpty())
            assertNull(store.getStringSet("PersistedSetValues"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun withTemporaryProperties(
        overrides: Map<String, String>,
        block: () -> Unit,
    ) {
        val originals = overrides.keys.associateWith { key -> System.getProperty(key) }
        try {
            overrides.forEach { (key, value) -> System.setProperty(key, value) }
            block()
        } finally {
            originals.forEach { (key, value) ->
                if (value == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, value)
                }
            }
        }
    }
}
