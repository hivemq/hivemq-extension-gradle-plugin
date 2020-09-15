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

/**
 * @author Lukas Brand
 * @author Silvio Giebl
 */
open class HivemqExtensionExtensionImpl : HivemqExtensionExtension {
    override var name: String? = null
    override var author: String? = null
    override var priority: Int = 0
    override var startPriority: Int = 1000
    override var mainClass: String? = null
    override var sdkVersion: String = "latest.integration"
    override var customJarTask: Any? = null
}