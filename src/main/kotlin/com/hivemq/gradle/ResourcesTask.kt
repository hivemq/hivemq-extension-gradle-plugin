package com.hivemq.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

open class ResourcesTask : DefaultTask() {

    private val extensionBuildFolder = project.buildDir.absolutePath + File.separator + "hivemq-extension"

    @TaskAction
    fun createResources() {
        val hiveMqExtensionExtension = project.extensions.getByType(HiveMqExtensionExtension::class.java)

        createExtensionXML(hiveMqExtensionExtension)
        createAdditionalResources(hiveMqExtensionExtension)
        getResourcesFromAddInResourcesTask()
    }

    private fun createExtensionXML(hiveMqExtensionExtension: HiveMqExtensionExtension) {
        val artifactId = project.name
        val version = project.version
        val extensionName = hiveMqExtensionExtension.extensionName
            ?: throw GradleException("hivemqExtension: extensionName attribute is missing.")
        val extensionAuthor = hiveMqExtensionExtension.extensionAuthor
            ?: throw GradleException("hivemqExtension: extensionAuthor attribute is missing.")
        val extensionPriority = hiveMqExtensionExtension.extensionPriority
            ?: 1_000

        val file = File(extensionBuildFolder, "hivemq-extension.xml")
        file.parentFile.mkdirs()
        file.writeText(
            """
                <hivemq-extension>
                    <id>${artifactId}</id>
                    <version>${version}</version>
                    <name>${extensionName}</name>
                    <author>${extensionAuthor}</author>
                    <priority>${extensionPriority}</priority>
                </hivemq-extension>
            """.trimIndent()
        )
    }

    private fun createAdditionalResources(hiveMqExtensionExtension: HiveMqExtensionExtension) {
        hiveMqExtensionExtension.additionalFiles?.run {
            forEach { (fromFile, toFile) ->
                Path.of(extensionBuildFolder, toFile).parent.toFile().mkdirs()
                val toFilePath = Path.of(extensionBuildFolder, toFile)
                Files.copy(
                    Path.of(fromFile),
                    toFilePath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    private fun getResourcesFromAddInResourcesTask() {
        inputs.files.files.forEach { file ->
            val filePath = Path.of(extensionBuildFolder, file.name)
            filePath.parent.toFile().mkdirs()
            Files.copy(file.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}