package com.hivemq.gradle

/**
 * @author Lukas Brand
 */
interface HiveMqExtensionExtension {

    //Required extension information
    var extensionName: String?
    var extensionAuthor: String?

    //Optional extension information
    var extensionPriority: Int?

    //Apply additional files via path or by a task
    var additionalFiles: Map<String, String>?
    var customResourcesTask: String?

    //Add a task to do something in between shadowing and zipping (i.e. proguard)
    var customJarTask: String?

    //The dependency HiveMQ Extension Sdk version
    var sdkVersion: String?
}