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
    fun test(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-extension"
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            version = "1.0.0"
            plugins {
                id("com.hivemq.extension")
            }
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
            .withGradleVersion("6.7")
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("hivemqExtensionZip")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionServiceDescriptor")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionXml")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":hivemqExtensionZip")?.outcome)

        val buildDir = projectDir.resolve("build/hivemq-extension")
        assertTrue(buildDir.resolve("com.hivemq.extension.sdk.api.ExtensionMain").exists())
        assertTrue(buildDir.resolve("test-extension-1.0.0.jar").exists())
        assertTrue(buildDir.resolve("hivemq-extension.xml").exists())
        assertTrue(buildDir.resolve("test-extension-1.0.0.zip").exists())
    }
}