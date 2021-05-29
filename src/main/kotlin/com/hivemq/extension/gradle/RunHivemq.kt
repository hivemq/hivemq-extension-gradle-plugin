package com.hivemq.extension.gradle

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec

/**
 * @author Silvio Giebl
 */
@Suppress("LeakingThis")
open class RunHivemq : JavaExec() {

    @Internal
    val hivemqHomeFolder = project.objects.directoryProperty()

    init {
        classpath(hivemqHomeFolder.file("bin/hivemq.jar"))
        systemProperty("hivemq.home", ToStringProvider(hivemqHomeFolder.map { it.asFile.absolutePath }))
        jvmArgs(
            "-Djava.net.preferIPv4Stack=true",
            "-noverify",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED",
            "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED"
        )
    }

    private class ToStringProvider(private val provider: Provider<String>) {
        override fun toString() = provider.get()
    }
}