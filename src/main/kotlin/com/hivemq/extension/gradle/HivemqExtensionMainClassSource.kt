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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * @author Silvio Giebl
 */
internal abstract class HivemqExtensionMainClassSource :
    ValueSource<String, HivemqExtensionMainClassSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val sources: ConfigurableFileCollection
    }

    final override fun obtain(): String? {
        val regex = Regex("[ ,:]ExtensionMain[ ,{]")
        var mainClass: String? = null
        parameters.sources.asFileTree.visit {
            if (!isDirectory && (name.endsWith(".java") || name.endsWith(".kt")) && file.readText().contains(regex)) {
                mainClass = relativePath.pathString.substringBeforeLast('.').replace('/', '.')
                stopVisiting()
            }
        }
        return mainClass
    }
}
