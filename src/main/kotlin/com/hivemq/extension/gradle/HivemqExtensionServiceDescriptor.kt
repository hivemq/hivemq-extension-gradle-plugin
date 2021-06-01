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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

/**
 * Task that generates a HiveMQ extension service descriptor.
 *
 * @author Silvio Giebl
 */
open class HivemqExtensionServiceDescriptor : DefaultTask() {

    companion object {
        const val EXTENSION_MAIN_CLASS_NAME: String = "com.hivemq.extension.sdk.api.ExtensionMain"
    }

    /**
     * Main class of the HiveMQ extension.
     */
    @Input
    val mainClass = project.objects.property<String>()

    /**
     * Configurable destination directory of the [serviceDescriptorFile].
     */
    @Internal
    val destinationDirectory = project.objects.directoryProperty()

    /**
     * Service descriptor file of the HiveMQ extension.
     */
    @OutputFile
    val serviceDescriptorFile = destinationDirectory.file(EXTENSION_MAIN_CLASS_NAME)

    @TaskAction
    protected fun run() {
        val serviceDescriptorFile = serviceDescriptorFile.get().asFile
        serviceDescriptorFile.parentFile.mkdirs()
        serviceDescriptorFile.writeText(mainClass.get())
    }
}