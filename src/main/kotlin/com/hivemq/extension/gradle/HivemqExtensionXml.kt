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

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

/**
 * Task that generates a HiveMQ extension xml descriptor.
 *
 * @author Silvio Giebl
 */
abstract class HivemqExtensionXml : DefaultTask() {

    companion object {
        const val EXTENSION_XML_NAME: String = "hivemq-extension.xml"
    }

    /**
     * ID of the HiveMQ extension.
     *
     * Defaults to the project name. Should not be changed without a specific reason.
     */
    @get:Input
    val id: Property<String> = project.objects.property<String>().convention(project.name)

    /**
     * Version of the HiveMQ extension.
     *
     * Defaults to the project version. Should not be changed without a specific reason.
     */
    @get:Input
    val version: Property<String> = project.objects.property<String>().convention(project.versionProvider)

    /**
     * Name of the HiveMQ extension.
     */
    @get:Input
    val name = project.objects.property<String>()

    /**
     * Author of the HiveMQ extension.
     */
    @get:Input
    val author = project.objects.property<String>()

    /**
     * Priority of the HiveMQ extension.
     */
    @get:Input
    val priority = project.objects.property<Int>()

    /**
     * Start priority of the HiveMQ extension.
     */
    @get:Input
    val startPriority = project.objects.property<Int>()

    /**
     * Configurable destination directory of the [xmlFile].
     */
    @get:Internal
    val destinationDirectory = project.objects.directoryProperty()

    /**
     * Property for [xmlFile] that is only publicly exposed as a provider.
     * It is defined as a protected property so this task is attached as a producer.
     */
    @get:OutputFile
    protected val xmlFileProperty = project.objects.fileProperty().value(destinationDirectory.file(EXTENSION_XML_NAME))

    /**
     * Xml descriptor file of the HiveMQ extension.
     */
    @get:Internal
    val xmlFile: Provider<RegularFile> = xmlFileProperty

    @TaskAction
    protected fun run() {
        val xmlFile = xmlFile.get().asFile
        xmlFile.parentFile.mkdirs()
        xmlFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <hivemq-extension>
                <id>${id.get()}</id>
                <version>${version.get()}</version>
                <name>${name.get()}</name>
                <author>${author.get()}</author>
                <priority>${priority.get()}</priority>
                <start-priority>${startPriority.get()}</start-priority>
            </hivemq-extension>
            """.trimIndent()
        )
    }
}