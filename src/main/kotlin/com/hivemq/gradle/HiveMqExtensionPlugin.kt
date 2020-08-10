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
        val extension = project.extensions
            .create(
                HiveMqExtensionExtension::class.java,
                "hivemq-extension",
                HiveMqExtensionExtensionImpl::class.java
            )

        configureJava(project)
        addDependencies(project, extension)

        val collectResources = project.tasks.register("createExtensionXml", ResourcesTask::class.java)
        collectResources.get().group = "hivemq extension"

        val shadow = applyShadowJarPlugin(project)

        val extensionJar = applyExtensionJarPlugin(project, shadow)

        applyDistributionPlugin(project, collectResources, extensionJar)
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

    private fun applyExtensionJarPlugin(project: Project, shadow: TaskProvider<ShadowJar>): TaskProvider<Task> {
        val extensionBuildFolder = project.buildDir.absolutePath + File.separator + "hivemq-extension"

        return project.tasks.register("hivemqExtensionJar") {
            it.group = "hivemq extension"
            it.outputs.file(File(extensionBuildFolder, "${project.name}-${project.version}.jar"))

            if (project.plugins.hasPlugin("enterprise-extension-plugin")) {
                it.dependsOn("signEnterpriseExtension")

                val path: Path = project.tasks.named("signEnterpriseExtension").get().outputs.files.singleFile.toPath()
                it.inputs.file(path)
            } else {
                it.dependsOn(shadow)
                it.inputs.file(shadow.get().outputs.files.singleFile.toPath())
            }

            it.doLast { _ ->
                Files.copy(
                    it.inputs.files.singleFile.toPath(),
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
            it.from(extensionBuildFolder)

            it.destinationDirectory.set(File(project.buildDir.absolutePath + File.separator + "distribution"))
            it.archiveFileName.set(project.name + "." + Zip.ZIP_EXTENSION)
        }
    }
}
