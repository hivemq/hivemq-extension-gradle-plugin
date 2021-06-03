package com.hivemq.extension.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider

fun <T> Project.memoizingProvider(initializer: () -> T): Provider<T> {
    val lazy = lazy(initializer)
    return provider { lazy.value }
}

val Project.versionProvider get() = memoizingProvider { version.toString() }