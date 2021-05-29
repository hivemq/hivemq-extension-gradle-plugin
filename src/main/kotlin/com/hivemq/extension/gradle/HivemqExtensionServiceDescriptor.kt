package com.hivemq.extension.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

/**
 * @author Silvio Giebl
 */
open class HivemqExtensionServiceDescriptor : DefaultTask() {

    companion object {
        const val EXTENSION_MAIN_CLASS_NAME: String = "com.hivemq.extension.sdk.api.ExtensionMain"
    }

    @Input
    val mainClass = project.objects.property<String>()

    @Internal
    val outputDirectory = project.objects.directoryProperty()

    @OutputFile
    val serviceDescriptorFile = outputDirectory.file(EXTENSION_MAIN_CLASS_NAME)

    @TaskAction
    protected fun run() {
        val serviceDescriptorFile = serviceDescriptorFile.get().asFile
        serviceDescriptorFile.parentFile.mkdirs()
        serviceDescriptorFile.writeText(mainClass.get())
    }
}