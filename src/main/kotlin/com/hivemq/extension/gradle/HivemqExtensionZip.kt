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
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.property

/**
 * Task that assembles a HiveMQ extension zip distribution.
 *
 * @author Silvio Giebl
 */
abstract class HivemqExtensionZip : Zip() {

    /**
     * Id of the HiveMQ extension.
     *
     * Defaults to the project name. Should not be changed without a specific reason.
     */
    @get:Internal
    val id = project.objects.property<String>().convention(project.name)

    /**
     * Version of the HiveMQ extension.
     *
     * Defaults to the project version. Should not be changed without a specific reason.
     */
    @get:Internal
    val version = project.objects.property<String>().convention(project.versionProvider)

    /**
     * Jar of the HiveMQ extension.
     */
    @get:Internal
    val jar = project.objects.fileProperty()

    init {
        archiveBaseName.set(id)
        archiveVersion.set(version)
        mainSpec.from(jar) { rename { "${id.get()}-${version.get()}.jar" } }
        rootSpec.into(id)
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}