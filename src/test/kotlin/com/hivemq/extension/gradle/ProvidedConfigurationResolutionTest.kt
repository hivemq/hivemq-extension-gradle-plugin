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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * The shadow jar task must not resolve the `hivemqProvided` configuration at configuration time.
 *
 * Gradle 9+ forbids resolving a configuration while the owning project's exclusive lock is not held.
 * A composite build that consumes the extension as an included (composite) build configures the
 * extension without holding its state lock, so resolving `hivemqProvided` eagerly inside the jar
 * task's configuration action fails the whole build with:
 *
 *   Resolution of the configuration ':...:hivemqProvided' was attempted without an exclusive lock.
 *   This is unsafe and not allowed.
 *
 * This is reproduced here by driving the extension's jar task from an outer build via `includeBuild`.
 */
internal class ProvidedConfigurationResolutionTest {

    @Test
    fun jarTaskConfiguration_doesNotResolveProvidedConfiguration(@TempDir projectDir: File) {
        // The plugin classpath that TestKit would normally inject into the build under test. We inject
        // it into the included build's buildscript so the plugin can be applied there.
        val pluginClasspath = GradleRunner.create().withPluginClasspath().pluginClasspath
            .joinToString(", ") { "\"${it.invariantSeparatorsPath}\"" }

        // ---- included build: a normal HiveMQ extension ----
        val extensionDir = projectDir.resolve("extension").apply { mkdirs() }
        extensionDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "extension"
            """.trimIndent()
        )
        extensionDir.resolve("build.gradle.kts").writeText(
            """
            buildscript {
                dependencies {
                    classpath(files($pluginClasspath))
                }
            }
            apply(plugin = "com.hivemq.extension")
            version = "1.0.0"
            the<com.hivemq.extension.gradle.HivemqExtensionExtension>().apply {
                name.set("Test Extension")
                author.set("HiveMQ")
                sdkVersion.set("4.17.0")
            }
            """.trimIndent()
        )
        extensionDir.resolve("src/main/java/test/TestExtensionMain.java").apply { parentFile.mkdirs() }.writeText(
            """
            package test;
            import com.hivemq.extension.sdk.api.ExtensionMain;
            import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
            import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
            import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
            import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
            public class TestExtensionMain implements ExtensionMain {
                @Override
                public void extensionStart(final ExtensionStartInput input, final ExtensionStartOutput output) {}
                @Override
                public void extensionStop(final ExtensionStopInput input, final ExtensionStopOutput output) {}
            }
            """.trimIndent()
        )

        // ---- outer build: consumes the extension as an included build (like the composite) ----
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "composite"
            includeBuild("extension")
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            tasks.register("buildExtension") {
                dependsOn(gradle.includedBuild("extension").task(":hivemqExtensionJar"))
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("buildExtension")
            .build()

        assertThat(result.output).doesNotContain("without an exclusive lock")
        assertThat(result.task(":extension:hivemqExtensionJar")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
