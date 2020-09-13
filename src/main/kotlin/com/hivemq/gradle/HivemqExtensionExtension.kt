package com.hivemq.gradle

/**
 * @author Lukas Brand
 * @author Silvio Giebl
 */
interface HivemqExtensionExtension {

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

    /**
     * Add a jar task to do something in between shadowing and zipping (i.e. proguard).
     * The task must produce exactly one jar file as output.
     * The task can be provided as [TaskProvider][org.gradle.api.tasks.TaskProvider] or [String].
     */
    var customJarTask: Any?
}