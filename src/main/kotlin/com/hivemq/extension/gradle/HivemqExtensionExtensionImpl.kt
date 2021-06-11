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

import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * @author Lukas Brand, Silvio Giebl
 */
abstract class HivemqExtensionExtensionImpl @Inject constructor(
    objectFactory: ObjectFactory,
    copySpecFactory: () -> CopySpec
) : HivemqExtensionExtension {

    override val name = objectFactory.property<String>()
    override val author = objectFactory.property<String>()
    override val priority = objectFactory.property<Int>().convention(0)
    override val startPriority = objectFactory.property<Int>().convention(1000)
    override val mainClass = objectFactory.property<String>()
    override val sdkVersion = objectFactory.property<String>().convention("latest.integration")
    final override val resources = copySpecFactory.invoke()

    init {
        resources.from("src/hivemq-extension")
        resources.duplicatesStrategy = DuplicatesStrategy.WARN
    }

    override fun resources(action: Action<in CopySpec>) {
        action.execute(resources)
    }
}