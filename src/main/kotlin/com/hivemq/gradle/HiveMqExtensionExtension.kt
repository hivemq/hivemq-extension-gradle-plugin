package com.hivemq.gradle

/**
 * @author Lukas Brand
 */
interface HiveMqExtensionExtension {
    var extensionName: String?
    var extensionAuthor: String?
    var additionalFiles: Map<String, String>?
    var sdkVersion: String?
}