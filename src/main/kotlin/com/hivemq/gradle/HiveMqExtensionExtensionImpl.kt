package com.hivemq.gradle

/**
 * @author Lukas Brand
 */
open class HiveMqExtensionExtensionImpl : HiveMqExtensionExtension {
    override var name: String? = null
    override var author: String? = null
    override var priority: Int? = null
    override var startPriority: Int? = null

    override var sdkVersion: String? = null

    override var customJarTask: String? = null
}