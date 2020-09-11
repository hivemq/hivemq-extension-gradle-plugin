package com.hivemq.gradle

/**
 * @author Lukas Brand
 */
interface HiveMqExtensionExtension {

    /**
     * Name of the HiveMQ extension, required
     */
    var name: String?

    /**
     * Author of the HiveMQ extension, required
     */
    var author: String?

    /**
     * Priority of the HiveMQ extension, default: 1000
     */
    var priority: Int

    /**
     * Start priority of the HiveMQ extension, default: 1000
     */
    var startPriority: Int

    /**
     * Main class of the HiveMQ extension SDK, required
     */
    var mainClass: String?

    /**
     * Version of the HiveMQ extension SDK, default: latest.integration
     */
    var sdkVersion: String

    //Add a task to do something in between shadowing and zipping (i.e. proguard)
//    var customJarTask: String?
}