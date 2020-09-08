package com.hivemq.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.*
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class HiveMqExtensionPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create(
            HiveMqExtensionExtension::class.java,
            "hivemqExtension",
            HiveMqExtensionExtensionImpl::class.java
        )

        configureJava(project)
        addDependencies(project, extension)

        project.afterEvaluate {

            val collectResources = project.tasks.register("collectExtensionResources", ResourcesTask::class.java)

            extension.customResourcesTask?.let { resourcesTaskString ->
                collectResources.get().dependsOn(resourcesTaskString)
                if (project.tasks.findByName(resourcesTaskString) == null) {
                    throw GradleException("The custom resource task \"${resourcesTaskString}\" does not exist.")
                }
                val resourceList = project.tasks.findByName(resourcesTaskString)!!.outputs.files.files.toList()
                collectResources.get().inputs.files(resourceList)
            }
            collectResources.get().group = "hivemq extension"

            val shadowTask = applyShadowJarPlugin(project)
            val resourcesTask = project.tasks.named("collectExtensionResources")

            if (extension.customJarTask != null) {
                val customJarTaskName = extension.customJarTask!!
                if (project.tasks.findByName(customJarTaskName) == null) {
                    throw GradleException("The custom jar task \"${customJarTaskName}\" does not exist.")
                }
                val customJarTask = project.tasks.named(customJarTaskName) {
                    it.dependsOn(shadowTask)
                    it.inputs.file(shadowTask.get().outputs.files.singleFile)
                }

                val zipTaskName = "extension" + customJarTaskName.removeSuffix("Jar").capitalize() + "Zip"
                createZipTask(project, zipTaskName, resourcesTask, customJarTask)
            }

            createZipTask(project, "extensionZip", resourcesTask, shadowTask)
        }
    }

    fun configureJava(project: Project) {
        if (!project.plugins.hasPlugin("java")) {
            project.plugins.apply(JavaPlugin::class.java)
        }

        project.extensions.configure("java", Action<JavaPluginExtension> {
            it.sourceCompatibility = JavaVersion.VERSION_11
            it.targetCompatibility = JavaVersion.VERSION_11
        })
    }

    fun addDependencies(project: Project, extension: HiveMqExtensionExtension) {
        project.afterEvaluate {
            project.repositories.mavenCentral()
            project.configurations.getByName("compileOnly").dependencies
                .add(
                    DefaultExternalModuleDependency(
                        "com.hivemq", "hivemq-extension-sdk", extension.sdkVersion
                            ?: "latest.integration"
                    )
                )
            project.configurations.getByName("testImplementation").dependencies
                .add(
                    DefaultExternalModuleDependency(
                        "com.hivemq", "hivemq-extension-sdk", extension.sdkVersion
                            ?: "latest.integration"
                    )
                )
        }
    }

    fun applyShadowJarPlugin(project: Project): TaskProvider<Task> {
        project.plugins.apply(ShadowPlugin::class.java)
        val convention = project.convention.getPlugin(JavaPluginConvention::class.java)

        project.tasks.create("extensionJar", ShadowJar::class.java) { extensionJarTask ->
            extensionJarTask.group = "hivemq extension"

            extensionJarTask.archiveAppendix.set("")
            extensionJarTask.archiveClassifier.set("shaded")

            extensionJarTask.manifest.inheritFrom((project.tasks.named("jar").get() as Jar).manifest)
            extensionJarTask.doFirst {
                val files = project.configurations.getByName("shadow").files
                if (files.isNotEmpty()) {
                    val libs =
                        mutableListOf((project.tasks.named("jar").get() as Jar).manifest.attributes["Class-Path"])
                    libs.addAll(files.map { it.name })
                    extensionJarTask.manifest.attributes(mapOf("Class-Path" to libs.joinToString(" ")))
                }
            }
            extensionJarTask.from(convention.sourceSets.named("main").get().output)
            extensionJarTask.configurations =
                if (project.configurations.findByName("runtimeClasspath") != null) {
                    listOf(project.configurations.named("runtimeClasspath").get())
                } else {
                    listOf(project.configurations.named("runtime").get())
                }
            extensionJarTask.exclude(
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "module-info.class"
            )
            project.artifacts.add("shadow", extensionJarTask)
        }
        return project.tasks.named("extensionJar")
    }

    fun createZipTask(
        project: Project,
        taskName: String,
        extensionResourcesTask: TaskProvider<Task>,
        extensionJarTask: TaskProvider<Task>
    ): TaskProvider<Zip> {
        val extensionBuildFolder = project.buildDir.absolutePath + File.separator + "hivemq-extension"

        return project.tasks.register(taskName, Zip::class.java) { zipTask ->
            zipTask.group = "hivemq extension"
            zipTask.dependsOn(extensionResourcesTask)
            zipTask.dependsOn(extensionJarTask)

            zipTask.doFirst {
                Files.copy(
                    extensionJarTask.get().outputs.files.singleFile.toPath(),
                    Path.of(extensionBuildFolder, "${project.name}-${project.version}.jar"),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            zipTask.from(extensionBuildFolder).into(project.name)
            zipTask.destinationDirectory.set(
                Path.of(project.buildDir.absolutePath, "distribution", taskName).toFile()
            )
            zipTask.archiveFileName.set(project.name + "." + Zip.ZIP_EXTENSION)
        }
    }
}
