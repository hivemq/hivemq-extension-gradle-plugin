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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * @author Silvio Giebl
 */
internal class MinRequiredGradleVersionTest {

    @Test
    fun gradle8(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-extension"
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.hivemq.extension")
            }
            version = "1.0.0"
            hivemqExtension {
                name.set("Test Extension")
                author.set("HiveMQ")
                sdkVersion.set("4.17.0")
            }
            """.trimIndent()
        )
        projectDir.resolve("src/main/java/test/TestExtensionMain.java").apply { parentFile.mkdirs() }.writeText(
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

        val result = GradleRunner.create()
            .withGradleVersion("8.0")
            .withProjectDir(projectDir)
            .withArguments("hivemqExtensionZip", "--init-script", System.getProperty("pluginTestInitScript"))
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionServiceDescriptor")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionXml")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionZip")?.outcome)

        val extensionBuildDir = projectDir.resolve("build/hivemq-extension")
        assertEquals(
            "test.TestExtensionMain",
            extensionBuildDir.resolve("com.hivemq.extension.sdk.api.ExtensionMain").readText(),
        )
        assertTrue(extensionBuildDir.resolve("test-extension-1.0.0.jar").exists())
        assertEquals(
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <hivemq-extension>
                <id>test-extension</id>
                <version>1.0.0</version>
                <name>Test Extension</name>
                <author>HiveMQ</author>
                <priority>0</priority>
                <start-priority>1000</start-priority>
            </hivemq-extension>
            """.trimIndent(),
            extensionBuildDir.resolve("hivemq-extension.xml").readText(),
        )
        assertTrue(extensionBuildDir.resolve("test-extension-1.0.0.zip").exists())
    }

    @Test
    fun gradle7(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-extension"
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.hivemq.extension")
            }
            buildscript {
                dependencies {
                    classpath("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
                }
                configurations.classpath {
                    exclude("com.github.johnrengelman", "shadow")
                }
            }
            version = "1.0.0"
            hivemqExtension {
                name.set("Test Extension")
                author.set("HiveMQ")
                sdkVersion.set("4.17.0")
            }
            """.trimIndent()
        )
        projectDir.resolve("src/main/java/test/TestExtensionMain.java").apply { parentFile.mkdirs() }.writeText(
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

        val result = GradleRunner.create()
            .withGradleVersion("7.0")
            .withProjectDir(projectDir)
            .withArguments("hivemqExtensionZip", "--init-script", System.getProperty("pluginTestInitScript"))
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionServiceDescriptor")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionXml")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionZip")?.outcome)

        val extensionBuildDir = projectDir.resolve("build/hivemq-extension")
        assertEquals(
            "test.TestExtensionMain",
            extensionBuildDir.resolve("com.hivemq.extension.sdk.api.ExtensionMain").readText(),
        )
        assertTrue(extensionBuildDir.resolve("test-extension-1.0.0.jar").exists())
        assertEquals(
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <hivemq-extension>
                <id>test-extension</id>
                <version>1.0.0</version>
                <name>Test Extension</name>
                <author>HiveMQ</author>
                <priority>0</priority>
                <start-priority>1000</start-priority>
            </hivemq-extension>
            """.trimIndent(),
            extensionBuildDir.resolve("hivemq-extension.xml").readText(),
        )
        assertTrue(extensionBuildDir.resolve("test-extension-1.0.0.zip").exists())
    }

    @Test
    fun gradle6(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-extension"
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.hivemq.extension")
            }
            buildscript {
                dependencies {
                    classpath("com.github.jengelman.gradle.plugins:shadow:6.1.0")
                }
                configurations.classpath {
                    exclude("com.github.johnrengelman", "shadow")
                }
            }
            version = "1.0.0"
            hivemqExtension {
                name.set("Test Extension")
                author.set("HiveMQ")
                sdkVersion.set("4.17.0")
            }
            """.trimIndent()
        )
        projectDir.resolve("src/main/java/test/TestExtensionMain.java").apply { parentFile.mkdirs() }.writeText(
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

        val result = GradleRunner.create()
            .withGradleVersion("6.9")
            .withProjectDir(projectDir)
            .withArguments("hivemqExtensionZip", "--init-script", System.getProperty("pluginTestInitScript"))
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionServiceDescriptor")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionXml")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionZip")?.outcome)

        val extensionBuildDir = projectDir.resolve("build/hivemq-extension")
        assertEquals(
            "test.TestExtensionMain",
            extensionBuildDir.resolve("com.hivemq.extension.sdk.api.ExtensionMain").readText(),
        )
        assertTrue(extensionBuildDir.resolve("test-extension-1.0.0.jar").exists())
        assertEquals(
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <hivemq-extension>
                <id>test-extension</id>
                <version>1.0.0</version>
                <name>Test Extension</name>
                <author>HiveMQ</author>
                <priority>0</priority>
                <start-priority>1000</start-priority>
            </hivemq-extension>
            """.trimIndent(),
            extensionBuildDir.resolve("hivemq-extension.xml").readText(),
        )
        assertTrue(extensionBuildDir.resolve("test-extension-1.0.0.zip").exists())
    }
}