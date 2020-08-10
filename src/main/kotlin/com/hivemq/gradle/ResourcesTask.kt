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
    }

    private fun createExtensionXML(hiveMqExtensionExtension: HiveMqExtensionExtension) {
        val artifactId = project.name
        val version = project.version
        val extensionName = hiveMqExtensionExtension.extensionName
            ?: throw GradleException("hivemq-extension: extensionName attribute is missing.")
        val extensionAuthor = hiveMqExtensionExtension.extensionAuthor
            ?: throw GradleException("hivemq-extension: extensionAuthor attribute is missing.")

        val file = File(extensionBuildFolder, "hivemq-extension.xml")
        file.parentFile.mkdirs()
        file.writeText(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "<!--\n" +
                    " ~ Copyright 2018 dc-square GmbH\n" +
                    " ~\n" +
                    " ~  Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    " ~  you may not use this file except in compliance with the License.\n" +
                    " ~  You may obtain a copy of the License at\n" +
                    " ~\n" +
                    " ~        http://www.apache.org/licenses/LICENSE-2.0\n" +
                    " ~\n" +
                    " ~  Unless required by applicable law or agreed to in writing, software\n" +
                    " ~  distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    " ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    " ~  See the License for the specific language governing permissions and\n" +
                    " ~  limitations under the License.\n" +
                    " -->\n" +
                    "\n" +
                    "<hivemq-extension>\n" +
                    "    <id>${artifactId}</id>\n" +
                    "    <version>${version}</version>\n" +
                    "    <name>${extensionName}</name>\n" +
                    "    <author>${extensionAuthor}</author>\n" +
                    "    <priority>1000</priority>\n" +
                    "</hivemq-extension>\n"
        )
    }

    private fun createAdditionalResources(hiveMqExtensionExtension: HiveMqExtensionExtension) {
        hiveMqExtensionExtension.additionalFiles?.let {
            it.forEach { (fromFile, toFile) ->
                Path.of(extensionBuildFolder, toFile).parent.toFile().mkdirs()
                Files.copy(
                    Path.of(fromFile),
                    Path.of(extensionBuildFolder, toFile),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }
}