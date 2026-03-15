package io.github.mrjoechen.oncekmpsample

import io.github.mrjoechen.Once
import io.github.mrjoechen.initialise

fun initialiseOnceKmpSample() {
    Once.initialise()
}

fun initialiseOnceKmpSample(appUpdatedTimeMillis: Long) {
    Once.initialise(appUpdatedTimeMillis = appUpdatedTimeMillis)
}
