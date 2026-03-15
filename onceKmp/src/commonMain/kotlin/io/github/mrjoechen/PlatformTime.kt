@file:OptIn(ExperimentalTime::class)

package io.github.mrjoechen

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
