package space.joechen.oncekmpsample

import android.content.Context
import space.joechen.Once
import space.joechen.initialise

fun initialiseOnceKmpSample() {
    Once.initialise()
}

fun initialiseOnceKmpSample(context: Context) {
    Once.initialise(context)
}
