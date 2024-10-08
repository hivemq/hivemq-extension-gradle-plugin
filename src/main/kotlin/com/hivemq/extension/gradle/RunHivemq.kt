/*
 * Copyright 2020-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.gradle

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec

/**
 * Runs HiveMQ.
 *
 * @author Silvio Giebl
 */
@Suppress("LeakingThis")
abstract class RunHivemq : JavaExec() {

    /**
     * HiveMQ home directory (unzipped).
     */
    @get:Internal
    val hivemqHomeDirectory = project.objects.directoryProperty()

    init {
        classpath(hivemqHomeDirectory.file("bin/hivemq.jar"))
        systemProperty("hivemq.home", ToStringProvider(hivemqHomeDirectory.map { it.asFile.absolutePath }))
        jvmArgs(
            "-Djava.net.preferIPv4Stack=true",
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