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
open class HivemqExtensionXml : DefaultTask() {

    companion object {
        const val EXTENSION_XML_NAME: String = "hivemq-extension.xml"
    }

    @Input
    val id = project.objects.property<String>().convention(project.name)

    @Input
    val version = project.objects.property<String>().convention(project.provider { project.version.toString() })

    @Input
    val name = project.objects.property<String>()

    @Input
    val author = project.objects.property<String>()

    @Input
    val priority = project.objects.property<Int>()

    @Input
    val startPriority = project.objects.property<Int>()

    @Internal
    val outputDirectory = project.objects.directoryProperty()

    @OutputFile
    val xmlFile = outputDirectory.file(EXTENSION_XML_NAME)

    @TaskAction
    protected fun run() {
        val xmlFile = xmlFile.get().asFile
        xmlFile.parentFile.mkdirs()
        xmlFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <hivemq-extension>
                <id>${id.get()}</id>
                <version>${version.get()}</version>
                <name>${name.get()}</name>
                <author>${author.get()}</author>
                <priority>${priority.get()}</priority>
                <start-priority>${startPriority.get()}</start-priority>
            </hivemq-extension>
            """.trimIndent()
        )
    }
}