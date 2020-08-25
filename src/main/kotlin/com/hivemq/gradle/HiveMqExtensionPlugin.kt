package com.hivemq.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.*
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
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
            val shadow = applyShadowJarPlugin(project)


            var outputTask: TaskProvider<Task> = project.tasks.named("shadowJar")

            if (extension.customJarTask != null) {
                val customJarTaskName = extension.customJarTask!!
                if (project.tasks.findByName(customJarTaskName) == null) {
                    throw GradleException("The custom jar task \"${customJarTaskName}\" does not exist.")
                }
                outputTask = project.tasks.named(customJarTaskName) {
                    it.dependsOn(shadow)
                    it.inputs.file(shadow.get().outputs.files.singleFile)
                }
            }
            val extensionJar = applyExtensionJarPlugin(project, outputTask)

            applyDistributionPlugin(project, collectResources, extensionJar)
        }
    }

    private fun configureJava(project: Project) {
        if (!project.plugins.hasPlugin("java")) {
            project.plugins.apply(JavaPlugin::class.java)
        }

        project.extensions.configure("java", Action<JavaPluginExtension> {
            it.sourceCompatibility = JavaVersion.VERSION_11
            it.targetCompatibility = JavaVersion.VERSION_11
        })
    }

    private fun addDependencies(project: Project, extension: HiveMqExtensionExtension) {
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

    private fun applyShadowJarPlugin(project: Project): TaskProvider<ShadowJar> {
        project.plugins.apply(ShadowPlugin::class.java)

        return project.tasks.named("shadowJar", ShadowJar::class.java) {
            it.group = "hivemq extension"

            it.archiveAppendix.set("")
            it.archiveClassifier.set("shaded")
            it.configurations = listOf(project.configurations.getByName("runtimeClasspath"))
        }
    }

    private fun applyExtensionJarPlugin(project: Project, precedingTask: TaskProvider<Task>): TaskProvider<Task> {
        val extensionBuildFolder = project.buildDir.absolutePath + File.separator + "hivemq-extension"

        return project.tasks.register("hivemqExtensionJar") {
            it.group = "hivemq extension"
            it.dependsOn(precedingTask)
            it.outputs.file(File(extensionBuildFolder, "${project.name}-${project.version}.jar"))
            it.inputs.file(precedingTask.get().outputs.files.singleFile)


            it.doLast { _ ->
                Files.copy(
                    it.inputs.files.files.maxBy { it.lastModified() }!!.toPath(),
                    Path.of(extensionBuildFolder, "${project.name}-${project.version}.jar"),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    private fun applyDistributionPlugin(
        project: Project,
        extensionResources: TaskProvider<ResourcesTask>,
        extensionJar: TaskProvider<Task>
    ) {
        project.plugins.apply(DistributionPlugin::class.java)

        project.tasks.register("zipExtension", Zip::class.java) {
            it.dependsOn(extensionResources)
            it.dependsOn(extensionJar)
            it.group = "hivemq extension"

            val extensionBuildFolder = project.buildDir.absolutePath + File.separator + "hivemq-extension"
            it.from(extensionBuildFolder).into(project.name)
            it.destinationDirectory.set(File(project.buildDir.absolutePath + File.separator + "distribution"))
            it.archiveFileName.set(project.name + "." + Zip.ZIP_EXTENSION)
        }
    }
}
