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

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.property

/**
 * Task that prepares a HiveMQ home directory with a HiveMQ extension for debugging.
 *
 * @author Silvio Giebl
 */
open class PrepareHivemqHome : Sync() {

    companion object {
        const val EXTENSIONS_FOLDER_NAME: String = "extensions"
    }

    /**
     * Id of the HiveMQ extension associated with the [hivemqExtensionZip].
     *
     * Defaults to the project name. Should not be changed without a specific reason.
     */
    @Internal
    val hivemqExtensionId = project.objects.property<String>().convention(project.name)

    /**
     * HiveMQ home directory (unzipped) used for debugging with the `runHivemqWithExtension` task.
     * The contents are copied to `build/hivemq-home`.
     */
    @Internal
    val hivemqHomeDirectory = project.objects.directoryProperty()

    @Internal
    val hivemqHomeDirectoryCopySpec = mainSpec.from(hivemqHomeDirectory) {
        exclude { it.path == "$EXTENSIONS_FOLDER_NAME/${hivemqExtensionId.get()}" }
    }

    /**
     * HiveMQ extension zip archive used for debugging with the `runHivemqWithExtension` task.
     * The contents are unzipped to `build/hivemq-home/extensions`.
     */
    @Internal
    val hivemqExtensionZip = project.objects.fileProperty()

    @Internal
    val hivemqExtensionZipCopySpec = mainSpec.from(hivemqExtensionZip.map { project.zipTree(it) }) {
        into(EXTENSIONS_FOLDER_NAME)
    }

    init {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}