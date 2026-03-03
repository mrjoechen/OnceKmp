package space.joechen

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

object Once {

    const val THIS_APP_INSTALL: Int = 0
    const val THIS_APP_VERSION: Int = 1
    const val THIS_APP_SESSION: Int = 2

    private const val TAG_LAST_SEEN_MAP_NAME = "TagLastSeenMap"
    private const val TODO_SET_NAME = "ToDoSet"

    private val lock = SynchronizedObject()

    private var lastAppUpdatedTime: Long = -1L
    private var tagLastSeenMap: PersistedMap? = null
    private var toDoSet: PersistedSet? = null
    private var sessionList: MutableList<String>? = null
    private var nowProvider: () -> Long = { currentTimeMillis() }

    fun initialise(
        storeFactory: OnceStoreFactory,
        appUpdatedTimeMillis: Long,
        nowProvider: () -> Long = { currentTimeMillis() },
    ) {
        synchronized(lock) {
            tagLastSeenMap = PersistedMap(storeFactory.createStore("PersistedMap$TAG_LAST_SEEN_MAP_NAME"))
            toDoSet = PersistedSet(storeFactory.createStore("PersistedSet$TODO_SET_NAME"))
            if (sessionList == null) {
                sessionList = mutableListOf()
            }
            lastAppUpdatedTime = appUpdatedTimeMillis
            this.nowProvider = nowProvider
        }
    }

    fun toDo(scope: Int, tag: String) {
        synchronized(lock) {
            when (scope) {
                THIS_APP_INSTALL -> {
                    if (requireTagLastSeenMap().get(tag).isEmpty()) {
                        requireToDoSet().put(tag)
                    }
                }
                THIS_APP_VERSION -> {
                    val hasBeenDoneThisVersion = requireTagLastSeenMap()
                        .get(tag)
                        .any { seenAt -> seenAt > lastAppUpdatedTime }
                    if (!hasBeenDoneThisVersion) {
                        requireToDoSet().put(tag)
                    }
                }
                THIS_APP_SESSION -> {
                    if (requireSessionList().none { sessionTag -> sessionTag == tag }) {
                        requireToDoSet().put(tag)
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unsupported scope: $scope")
                }
            }
        }
    }

    fun toDo(tag: String) {
        synchronized(lock) {
            requireToDoSet().put(tag)
        }
    }

    fun needToDo(tag: String): Boolean = synchronized(lock) {
        requireToDoSet().contains(tag)
    }

    fun lastDone(tag: String): Long? = synchronized(lock) {
        val lastSeenTimestamps = requireTagLastSeenMap().get(tag)
        if (lastSeenTimestamps.isEmpty()) {
            null
        } else {
            lastSeenTimestamps.last()
        }
    }

    fun beenDone(tag: String): Boolean = beenDone(THIS_APP_INSTALL, tag, Amount.moreThan(0))

    fun beenDone(tag: String, numberOfTimes: CountChecker): Boolean = beenDone(
        scope = THIS_APP_INSTALL,
        tag = tag,
        numberOfTimes = numberOfTimes,
    )

    fun beenDone(scope: Int, tag: String): Boolean = beenDone(scope, tag, Amount.moreThan(0))

    fun beenDone(scope: Int, tag: String, numberOfTimes: CountChecker): Boolean = synchronized(lock) {
        if (scope != THIS_APP_INSTALL && scope != THIS_APP_VERSION && scope != THIS_APP_SESSION) {
            throw IllegalArgumentException("Unsupported scope: $scope")
        }

        val tagSeenDates = requireTagLastSeenMap().get(tag)
        if (tagSeenDates.isEmpty()) {
            return@synchronized false
        }

        return@synchronized when (scope) {
            THIS_APP_INSTALL -> numberOfTimes.check(tagSeenDates.size)
            THIS_APP_SESSION -> {
                var counter = 0
                for (sessionTag in requireSessionList().toList()) {
                    if (sessionTag == tag) {
                        counter++
                    }
                }
                numberOfTimes.check(counter)
            }
            THIS_APP_VERSION -> {
                var counter = 0
                for (seenDate in tagSeenDates) {
                    if (seenDate > lastAppUpdatedTime) {
                        counter++
                    }
                }
                numberOfTimes.check(counter)
            }
            else -> false
        }
    }

    fun beenDone(timeUnit: OnceTimeUnit, amount: Long, tag: String): Boolean {
        return beenDone(timeUnit, amount, tag, Amount.moreThan(0))
    }

    fun beenDone(
        timeUnit: OnceTimeUnit,
        amount: Long,
        tag: String,
        numberOfTimes: CountChecker,
    ): Boolean {
        val timeInMillis = timeUnit.toMillis(amount)
        return beenDone(timeInMillis, tag, numberOfTimes)
    }

    fun beenDone(timeSpanInMillis: Long, tag: String): Boolean {
        return beenDone(timeSpanInMillis, tag, Amount.moreThan(0))
    }

    fun beenDone(
        timeSpanInMillis: Long,
        tag: String,
        numberOfTimes: CountChecker,
    ): Boolean = synchronized(lock) {
        val tagSeenDates = requireTagLastSeenMap().get(tag)
        if (tagSeenDates.isEmpty()) {
            return@synchronized false
        }

        var counter = 0
        val sinceCheckTime = nowProvider() - timeSpanInMillis
        for (seenDate in tagSeenDates) {
            if (seenDate > sinceCheckTime) {
                counter++
            }
        }
        return@synchronized numberOfTimes.check(counter)
    }

    fun markDone(tag: String) {
        synchronized(lock) {
            requireTagLastSeenMap().put(tag, nowProvider())
            requireSessionList().add(tag)
            requireToDoSet().remove(tag)
        }
    }

    fun clearDone(tag: String) {
        synchronized(lock) {
            requireTagLastSeenMap().remove(tag)
            requireSessionList().removeAll { sessionTag -> sessionTag == tag }
        }
    }

    fun clearToDo(tag: String) {
        synchronized(lock) {
            requireToDoSet().remove(tag)
        }
    }

    fun clearAll() {
        synchronized(lock) {
            requireTagLastSeenMap().clear()
            requireSessionList().clear()
        }
    }

    fun clearAllToDos() {
        synchronized(lock) {
            requireToDoSet().clear()
        }
    }

    private fun requireTagLastSeenMap(): PersistedMap {
        return requireNotNull(tagLastSeenMap) {
            "Once has not been initialised. Call Once.initialise(...) before using it."
        }
    }

    private fun requireToDoSet(): PersistedSet {
        return requireNotNull(toDoSet) {
            "Once has not been initialised. Call Once.initialise(...) before using it."
        }
    }

    private fun requireSessionList(): MutableList<String> {
        return requireNotNull(sessionList) {
            "Once has not been initialised. Call Once.initialise(...) before using it."
        }
    }
}
