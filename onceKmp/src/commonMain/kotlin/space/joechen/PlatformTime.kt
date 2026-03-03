@file:OptIn(ExperimentalTime::class)

package space.joechen

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
