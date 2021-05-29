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

import org.gradle.api.provider.Property

/**
 * Gradle extension for configuring the HiveMQ extension build.
 *
 * @author Lukas Brand, Silvio Giebl
 */
interface HivemqExtensionExtension {

    /**
     * Name of the HiveMQ extension, required
     */
    val name: Property<String>

    /**
     * Author of the HiveMQ extension, required
     */
    val author: Property<String>

    /**
     * Priority of the HiveMQ extension, default: 0
     */
    val priority: Property<Int>

    /**
     * Start priority of the HiveMQ extension, default: 1000
     */
    val startPriority: Property<Int>

    /**
     * Main class of the HiveMQ extension, will be determined automatically if not set
     */
    val mainClass: Property<String>

    /**
     * Version of the HiveMQ extension SDK, recommended to specify, default: latest.integration
     */
    val sdkVersion: Property<String>

    /**
     * Add a jar task to do something in between shadowing and zipping (i.e. proguard).
     * The task must produce exactly one jar file as output.
     * The task can be provided as [TaskProvider][org.gradle.api.tasks.TaskProvider] or [String].
     */
    var customJarTask: Any?
}