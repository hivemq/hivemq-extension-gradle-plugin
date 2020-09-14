package com.hivemq.gradle

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