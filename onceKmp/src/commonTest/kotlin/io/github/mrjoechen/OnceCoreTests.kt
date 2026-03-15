package io.github.mrjoechen

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnceCoreTests {

    private val tagUnderTest = "testTag"
    private lateinit var storeFactory: InMemoryStoreFactory
    private lateinit var clock: TestClock

    @BeforeTest
    fun setup() {
        storeFactory = InMemoryStoreFactory()
        clock = TestClock(1_000L)
        Once.initialise(
            storeFactory = storeFactory,
            appUpdatedTimeMillis = 0L,
            nowProvider = clock::now,
        )
        Once.clearAll()
        Once.clearAllToDos()
    }

    @AfterTest
    fun cleanup() {
        Once.clearAll()
        Once.clearAllToDos()
    }

    @Test
    fun unseenTags() {
        assertFalse(Once.beenDone(Once.THIS_APP_SESSION, tagUnderTest))
        assertFalse(Once.beenDone(Once.THIS_APP_INSTALL, tagUnderTest))
        assertFalse(Once.beenDone(Once.THIS_APP_VERSION, tagUnderTest))
        assertFalse(Once.beenDone(OnceTimeUnit.DAYS, 1, tagUnderTest))
    }

    @Test
    fun seenTagImmediately() {
        Once.markDone(tagUnderTest)

        assertTrue(Once.beenDone(Once.THIS_APP_SESSION, tagUnderTest))
        assertTrue(Once.beenDone(Once.THIS_APP_INSTALL, tagUnderTest))
        assertTrue(Once.beenDone(Once.THIS_APP_VERSION, tagUnderTest))
        assertTrue(Once.beenDone(OnceTimeUnit.MINUTES, 1, tagUnderTest))
    }

    @Test
    fun removeFromDone() {
        Once.markDone(tagUnderTest)
        Once.clearDone(tagUnderTest)

        assertFalse(Once.beenDone(Once.THIS_APP_SESSION, tagUnderTest))
        assertFalse(Once.beenDone(Once.THIS_APP_INSTALL, tagUnderTest))
        assertFalse(Once.beenDone(Once.THIS_APP_VERSION, tagUnderTest))
        assertFalse(Once.beenDone(OnceTimeUnit.DAYS, 1, tagUnderTest))
    }

    @Test
    fun clearDoneRemovesAllSessionOccurrences() {
        val tag = "clear done session"
        Once.markDone(tag)
        Once.markDone(tag)
        assertTrue(Once.beenDone(Once.THIS_APP_SESSION, tag, Amount.exactly(2)))

        Once.clearDone(tag)
        Once.markDone(tag)
        assertTrue(Once.beenDone(Once.THIS_APP_SESSION, tag, Amount.exactly(1)))
    }

    @Test
    fun seenTagAfterAppUpdate() {
        Once.markDone(tagUnderTest)

        Once.initialise(
            storeFactory = storeFactory,
            appUpdatedTimeMillis = clock.now() + 1_000L,
            nowProvider = clock::now,
        )

        assertTrue(Once.beenDone(Once.THIS_APP_SESSION, tagUnderTest))
        assertTrue(Once.beenDone(Once.THIS_APP_INSTALL, tagUnderTest))
        assertFalse(Once.beenDone(Once.THIS_APP_VERSION, tagUnderTest))
        assertTrue(Once.beenDone(OnceTimeUnit.MINUTES, 1, tagUnderTest))
    }

    @Test
    fun seenTagAfterSecond() {
        Once.markDone(tagUnderTest)
        clock.advance(1_001L)

        assertFalse(Once.beenDone(OnceTimeUnit.SECONDS, 1, tagUnderTest))
        assertFalse(Once.beenDone(1_000L, tagUnderTest))
    }

    @Test
    fun beenDoneMultipleTimes() {
        val tag = "action several times"
        Once.markDone(tag)
        Once.markDone(tag)
        assertFalse(Once.beenDone(tag, Amount.exactly(3)))

        Once.markDone(tag)
        assertTrue(Once.beenDone(tag, Amount.exactly(3)))
    }

    @Test
    fun beenDoneMultipleTimesAcrossScopes() {
        val tag = "across scopes"
        Once.markDone(tag)
        clock.advance(1_000L)

        Once.initialise(
            storeFactory = storeFactory,
            appUpdatedTimeMillis = 1_500L,
            nowProvider = clock::now,
        )
        Once.markDone(tag)

        assertTrue(Once.beenDone(Once.THIS_APP_INSTALL, tag, Amount.exactly(2)))
        assertFalse(Once.beenDone(Once.THIS_APP_VERSION, tag, Amount.exactly(2)))

        Once.markDone(tag)
        assertTrue(Once.beenDone(Once.THIS_APP_INSTALL, tag, Amount.exactly(3)))
        assertTrue(Once.beenDone(Once.THIS_APP_VERSION, tag, Amount.exactly(2)))
    }

    @Test
    fun beenDoneDifferentCountCheckers() {
        val tag = "count checks"
        Once.markDone(tag)
        Once.markDone(tag)
        Once.markDone(tag)

        assertTrue(Once.beenDone(tag, Amount.moreThan(-1)))
        assertTrue(Once.beenDone(tag, Amount.moreThan(2)))
        assertFalse(Once.beenDone(tag, Amount.moreThan(3)))

        assertTrue(Once.beenDone(tag, Amount.lessThan(10)))
        assertTrue(Once.beenDone(tag, Amount.lessThan(4)))
        assertFalse(Once.beenDone(tag, Amount.lessThan(3)))
    }

    @Test
    fun beenDoneMultipleTimesWithTimeSpans() {
        Once.markDone(tagUnderTest)
        clock.advance(1_000L)
        Once.markDone(tagUnderTest)

        assertTrue(Once.beenDone(OnceTimeUnit.SECONDS, 3, tagUnderTest, Amount.exactly(2)))
        assertTrue(Once.beenDone(OnceTimeUnit.SECONDS, 1, tagUnderTest, Amount.exactly(1)))
    }

    @Test
    fun lastDone() {
        assertNull(Once.lastDone(tagUnderTest))
        Once.markDone(tagUnderTest)
        assertEquals(clock.now(), Once.lastDone(tagUnderTest))
    }

    @Test
    fun clearAll() {
        Once.markDone("tag1")
        Once.markDone("tag2")
        Once.clearAll()

        assertFalse(Once.beenDone(Once.THIS_APP_INSTALL, "tag1"))
        assertFalse(Once.beenDone(Once.THIS_APP_INSTALL, "tag2"))
    }

    @Test
    fun emptyTag() {
        assertFalse(Once.beenDone(""))
        Once.markDone("")
        assertTrue(Once.beenDone(""))
    }

    @Test
    fun todo() {
        val task = "todo task"

        assertFalse(Once.needToDo(task))
        Once.toDo(task)
        assertTrue(Once.needToDo(task))
        assertFalse(Once.beenDone(task))

        Once.markDone(task)
        assertFalse(Once.needToDo(task))
        assertTrue(Once.beenDone(task))
    }

    @Test
    fun todoThisSessionAndInstall() {
        val tag = "todo session install"
        Once.toDo(Once.THIS_APP_SESSION, tag)
        assertTrue(Once.needToDo(tag))

        Once.markDone(tag)
        Once.toDo(Once.THIS_APP_SESSION, tag)
        assertFalse(Once.needToDo(tag))

        Once.toDo(Once.THIS_APP_INSTALL, tag)
        assertFalse(Once.needToDo(tag))

        Once.toDo(tag)
        assertTrue(Once.needToDo(tag))
    }

    @Test
    fun todoThisAppVersion() {
        val tag = "todo this app version"

        Once.toDo(Once.THIS_APP_VERSION, tag)
        assertTrue(Once.needToDo(tag))
        Once.markDone(tag)
        assertFalse(Once.needToDo(tag))

        Once.toDo(Once.THIS_APP_VERSION, tag)
        assertFalse(Once.needToDo(tag))

        Once.initialise(
            storeFactory = storeFactory,
            appUpdatedTimeMillis = clock.now() + 10L,
            nowProvider = clock::now,
        )
        Once.toDo(Once.THIS_APP_VERSION, tag)
        assertTrue(Once.needToDo(tag))
    }

    @Test
    fun unsupportedScopeThrows() {
        assertFailsWith<IllegalArgumentException> {
            Once.beenDone(-1, "invalid")
        }
        assertFailsWith<IllegalArgumentException> {
            Once.toDo(-1, "invalid")
        }
    }

    @Test
    fun malformedPersistedTimestampListDoesNotCrash() {
        val rawStore = storeFactory.createStore("PersistedMapTagLastSeenMap")
        rawStore.putString("malformed", "1000,invalid,3000")

        Once.initialise(
            storeFactory = storeFactory,
            appUpdatedTimeMillis = 0L,
            nowProvider = clock::now,
        )

        assertTrue(Once.beenDone("malformed", Amount.exactly(2)))
    }

    private class TestClock(initialValue: Long) {
        private var value: Long = initialValue

        fun now(): Long = value

        fun advance(millis: Long) {
            value += millis
        }
    }
}
