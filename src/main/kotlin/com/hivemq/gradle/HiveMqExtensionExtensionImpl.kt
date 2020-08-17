package com.hivemq.gradle

/**
 * @author Lukas Brand
 */
open class HiveMqExtensionExtensionImpl : HiveMqExtensionExtension {
    override var extensionName: String? = null
    override var extensionAuthor: String? = null
    override var extensionPriority: Int? = null

    override var additionalFiles: Map<String, String>? = null
    override var customResourcesTask: String? = null

    override var customJarTask: String? = null

    override var sdkVersion: String? = null
}