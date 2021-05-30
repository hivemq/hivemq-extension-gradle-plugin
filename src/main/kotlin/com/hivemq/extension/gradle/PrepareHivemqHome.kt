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

import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.property

/**
 * @author Silvio Giebl
 */
open class PrepareHivemqHome : Sync() {

    companion object {
        const val EXTENSIONS_FOLDER_NAME: String = "extensions"
    }

    @Internal
    val extensionId = project.objects.property<String>().convention(project.name)

    /**
     * Specifies the path to a HiveMQ directory (unzipped).
     * The contents are copied to `build/hivemq-home` which is used by the `runHivemqWithExtension` task as the hivemq
     * home folder.
     *
     * Can be any type allowed by [org.gradle.api.Project.file].
     */
    @Internal
    val hivemqFolder = project.objects.property<Any>()

    @Internal
    val hivemqFolderCopySpec = mainSpec.from(hivemqFolder) {
        exclude { it.path == "$EXTENSIONS_FOLDER_NAME/${extensionId.get()}" }
    }

    /**
     * Specifies the [Zip] task that builds the current HiveMQ extension zip archive.
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

    @TaskAction
    override fun copy() {
        if (!project.file(hivemqFolder.get()).exists()) {
            throw GradleException("hivemqFolder ${hivemqFolder.get()} does not exist")
        }
        super.copy()
    }
}