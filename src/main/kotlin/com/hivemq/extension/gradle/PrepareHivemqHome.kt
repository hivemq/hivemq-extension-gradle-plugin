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

import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.property

/**
 * @author Silvio Giebl
 */
open class PrepareHivemqHome : Sync() {

    /**
     * Specifies the path to an HiveMQ directory (unzipped).
     * The contents are copied to <code>build/hivemq-home</code> which is used by the
     * <code>runHivemqWithExtension</code> task as the hivemq home folder.
     *
     * Can be any type allowed by [org.gradle.api.Project.file].
     */
    val hivemqFolder = project.objects.property<Any>()

    /**
     * Specifies the [Zip] task that builds the current HiveMQ extension zip archive.
     * The contents are unzipped to <code>build/hivemq-home/extensions</code>.
     */
    val extensionZipTask = project.objects.property<Zip>()
}