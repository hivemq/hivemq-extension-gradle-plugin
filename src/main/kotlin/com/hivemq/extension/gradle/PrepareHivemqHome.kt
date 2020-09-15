package com.hivemq.extension.gradle

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

/**
 * @author Silvio Giebl
 */
open class PrepareHivemqHome : Sync() {

    val hivemqFolder: Property<Any> = project.objects.property(Any::class.java)
    val extensionZipTask: Property<Zip> = project.objects.property(Zip::class.java)
}