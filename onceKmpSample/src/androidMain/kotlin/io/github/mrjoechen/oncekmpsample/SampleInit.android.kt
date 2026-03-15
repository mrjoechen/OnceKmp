package io.github.mrjoechen.oncekmpsample

import android.content.Context
import io.github.mrjoechen.Once
import io.github.mrjoechen.initialise

fun initialiseOnceKmpSample() {
    Once.initialise()
}

fun initialiseOnceKmpSample(context: Context) {
    Once.initialise(context)
}
