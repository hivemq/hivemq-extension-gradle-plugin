package com.hivemq.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

open class HivemqJarTask : DefaultTask() {

    @OutputFile
    val outJarFile: Path = Path.of(project.buildDir.absolutePath, "${project.name}-${project.version}.jar")


    @TaskAction
    fun renameJar() {
        Files.copy(this.inputs.files.singleFile.toPath(), outJarFile, StandardCopyOption.REPLACE_EXISTING)
    }
}