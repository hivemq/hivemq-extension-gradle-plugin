package com.hivemq.extension.gradle

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.property

/**
 * @author Silvio Giebl
 */
open class HivemqExtensionZip : Zip() {

    @Internal
    val id = project.objects.property<String>().convention(project.name)

    @Internal
    val version = project.objects.property<String>().convention(project.provider { project.version.toString() })

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