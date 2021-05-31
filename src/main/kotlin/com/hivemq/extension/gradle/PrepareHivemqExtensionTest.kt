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

/**
 * Task that prepares a HiveMQ extension for integration testing.
 *
 * @author Silvio Giebl
 */
open class PrepareHivemqExtensionTest : Sync() {

    /**
     * HiveMQ extension zip archive used for integration testing.
     * The contents are unzipped to `build/hivemq-extension-test`.
     */
    @Internal
    val hivemqExtensionZip = project.objects.fileProperty()

    @Internal
    val hivemqExtensionZipCopySpec = mainSpec.from(hivemqExtensionZip.map { project.zipTree(it) }) {}

    init {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}