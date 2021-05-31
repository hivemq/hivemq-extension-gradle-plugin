package com.hivemq.extension.gradle

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.property

/**
 * Task that builds a HiveMQ extension zip archive.
 *
 * @author Silvio Giebl
 */
open class HivemqExtensionZip : Zip() {

    /**
     * Id of the HiveMQ extension.
     *
     * Defaults to the project name. Should not be changed without a specific reason.
     */
    @Internal
    val id = project.objects.property<String>().convention(project.name)

    /**
     * Version of the HiveMQ extension.
     *
     * Defaults to the project version. Should not be changed without a specific reason.
     */
    @Internal
    val version = project.objects.property<String>().convention(project.provider { project.version.toString() })

    /**
     * Jar of the HiveMQ extension.
     */
    @Internal
    val jar = project.objects.fileProperty()

    init {
        archiveBaseName.set(id)
        archiveVersion.set(version)
        mainSpec.from(jar) { rename { "${id.get()}-${version.get()}.jar" } }
        rootSpec.into(id)
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}