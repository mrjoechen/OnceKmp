package space.joechen.oncekmpsample

import space.joechen.Once
import space.joechen.initialise

fun initialiseOnceKmpSample() {
    Once.initialise()
}

fun initialiseOnceKmpSample(appUpdatedTimeMillis: Long) {
    Once.initialise(appUpdatedTimeMillis = appUpdatedTimeMillis)
}
