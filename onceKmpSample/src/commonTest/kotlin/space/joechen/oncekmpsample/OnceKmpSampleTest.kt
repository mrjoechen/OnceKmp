package space.joechen.oncekmpsample

import space.joechen.InMemoryStoreFactory
import space.joechen.Once
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnceKmpSampleTest {
    private lateinit var sample: OnceKmpSample
    private lateinit var clock: TestClock

    @BeforeTest
    fun setup() {
        clock = TestClock(1_000L)
        Once.initialise(
            storeFactory = InMemoryStoreFactory(),
            appUpdatedTimeMillis = 0L,
            nowProvider = clock::now,
        )
        Once.clearAll()
        Once.clearAllToDos()
        sample = OnceKmpSample()
    }

    @AfterTest
    fun tearDown() {
        Once.clearAll()
        Once.clearAllToDos()
    }

    @Test
    fun showWhatsNewOnlyOncePerVersion() {
        assertTrue(sample.shouldShowWhatsNewThisVersion())
        sample.markWhatsNewShown()
        assertFalse(sample.shouldShowWhatsNewThisVersion())
    }

    @Test
    fun ratePromptOnThirdAction() {
        assertFalse(sample.trackCoreActionAndShouldPromptRating())
        assertFalse(sample.trackCoreActionAndShouldPromptRating())
        assertTrue(sample.trackCoreActionAndShouldPromptRating())
    }

    private class TestClock(initialTime: Long) {
        private var now: Long = initialTime

        fun now(): Long = now
    }
}
