/*
 * Copyright 2020-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion

/**
 * @author Lukas Brand, Silvio Giebl
 */
class HivemqExtensionPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME: String = "hivemqExtension"
        const val GROUP_NAME: String = "hivemq extension"
        const val BUILD_FOLDER_NAME: String = "hivemq-extension"
        const val TASK_PREFIX: String = "hivemqExtension"
        const val JAR_SUFFIX: String = "Jar"
        const val ZIP_SUFFIX: String = "Zip"
        const val SERVICE_DESCRIPTOR_TASK_NAME: String = "${TASK_PREFIX}ServiceDescriptor"
        const val XML_TASK_NAME: String = "${TASK_PREFIX}Xml"
        const val PROVIDED_CONFIGURATION_NAME: String = "hivemqProvided"

        const val PREPARE_HIVEMQ_HOME_TASK_NAME: String = "prepareHivemqHome"
        const val RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME: String = "runHivemqWithExtension"
        const val HIVEMQ_HOME_FOLDER_NAME: String = "hivemq-home"

        const val INTEGRATION_TEST_SOURCE_SET_NAME = "integrationTest"
        const val INTEGRATION_TEST_TASK_NAME = "integrationTest"
        const val PREPARE_HIVEMQ_EXTENSION_TEST_TASK_NAME = "prepareHivemqExtensionTest"
        const val HIVEMQ_EXTENSION_TEST_FOLDER_NAME = "hivemq-extension-test"
    }

    override fun apply(project: Project) {
        configureJava(project)
        val extension = createExtension(project)
        addDependencies(project, extension)
        val jarTask = registerJarTask(project, extension)
        registerXmlTask(project, extension)
        val zipTask = registerZipTask(project, extension, jarTask.flatMap { it.archiveFile })

        setupDebugging(project, zipTask.flatMap { it.archiveFile })
        setupIntegrationTesting(project, zipTask.flatMap { it.archiveFile })
    }

    fun configureJava(project: Project) {
        project.plugins.apply(JavaPlugin::class)

        project.extensions.configure<JavaPluginExtension> {
            if (GradleVersion.current() >= GradleVersion.version("6.7")) {
                toolchain.languageVersion.set(JavaLanguageVersion.of(11))
            } else {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }

    fun createExtension(project: Project): HivemqExtensionExtension {
        return project.extensions.create(
            HivemqExtensionExtension::class,
            EXTENSION_NAME,
            HivemqExtensionExtensionImpl::class,
            { project.copySpec() },
        ).apply {
            mainClass.convention(project.memoizingProvider { findMainClass(project) })
        }
    }

    fun addConfiguration(project: Project): Configuration {
        val providedConfiguration = project.configurations.create(PROVIDED_CONFIGURATION_NAME) {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
        project.configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME) {
            extendsFrom(providedConfiguration)
        }
        return providedConfiguration
    }

    fun addRepositories(project: Project) {
        if (project.repositories.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
            project.repositories.mavenCentral()
        }
    }

    private fun addDependencies(project: Project, extension: HivemqExtensionExtension) {
        addConfiguration(project).withDependencies {
            add(project.dependencies.create("com.hivemq:hivemq-extension-sdk:${extension.sdkVersion.get()}"))
        }
        project.afterEvaluate {
            addRepositories(project)
        }
    }

    fun registerJarTask(
        project: Project,
        extension: HivemqExtensionExtension,
        classifier: String = "",
    ): TaskProvider<ShadowJar> {

        val serviceDescriptorTask = registerServiceDescriptorTask(project, extension)
        project.tasks.named<Copy>(JavaPlugin.PROCESS_RESOURCES_TASK_NAME) {
            from(serviceDescriptorTask) { into("META-INF/services") }
        }

        return project.tasks.register<ShadowJar>(TASK_PREFIX + classifier.replaceFirstChar(Char::uppercaseChar) + JAR_SUFFIX) {
            group = GROUP_NAME
            description =
                "Assembles the ${if (classifier.isEmpty()) "" else "$classifier "}jar of the HiveMQ extension."

            archiveClassifier.set(classifier)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))

            manifest.inheritFrom(project.tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME).get().manifest)
            from(project.the<SourceSetContainer>()[SourceSet.MAIN_SOURCE_SET_NAME].output)
            configurations = listOf(project.configurations[JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME])
            val providedConfiguration = project.configurations[PROVIDED_CONFIGURATION_NAME]
            for (component in providedConfiguration.incoming.resolutionResult.allComponents) {
                val id = component.moduleVersion
                if (id != null) {
                    dependencyFilter.exclude(dependencyFilter.dependency("${id.group}:${id.name}"))
                }
            }
            exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
        }
    }

    private fun registerServiceDescriptorTask(
        project: Project,
        extension: HivemqExtensionExtension,
    ): TaskProvider<HivemqExtensionServiceDescriptor> {
        return project.tasks.register<HivemqExtensionServiceDescriptor>(SERVICE_DESCRIPTOR_TASK_NAME) {
            group = GROUP_NAME
            description = "Generates the service descriptor of the HiveMQ extension."

            mainClass.set(extension.mainClass)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))
        }
    }

    private fun findMainClass(project: Project): String? {
        val regex = Regex("[ ,:]ExtensionMain[ ,{]")
        var mainClass: String? = null
        project.the<SourceSetContainer>()[SourceSet.MAIN_SOURCE_SET_NAME].allSource.visit {
            if (!isDirectory && (file.name.endsWith(".java") || file.name.endsWith(".kt")) &&
                file.readText().contains(regex)
            ) {
                mainClass = relativePath.pathString.substringBeforeLast('.').replace('/', '.')
                stopVisiting()
            }
        }
        return mainClass
    }

    fun registerXmlTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<HivemqExtensionXml> {
        return project.tasks.register<HivemqExtensionXml>(XML_TASK_NAME) {
            group = GROUP_NAME
            description = "Generates the xml descriptor of the HiveMQ extension."

            name.set(extension.name)
            author.set(extension.author)
            priority.set(extension.priority)
            startPriority.set(extension.startPriority)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))
        }.also {
            extension.resources.from(it)
        }
    }

    fun registerZipTask(
        project: Project,
        extension: HivemqExtensionExtension,
        jarProvider: Provider<RegularFile>,
        classifier: String = ""
    ): TaskProvider<HivemqExtensionZip> {
        return project.tasks.register<HivemqExtensionZip>(TASK_PREFIX + classifier.replaceFirstChar(Char::uppercaseChar) + ZIP_SUFFIX) {
            group = GROUP_NAME
            description =
                "Assembles the zip distribution of the HiveMQ extension${if (classifier.isEmpty()) "" else " containing the $classifier jar"}."

            jar.set(jarProvider)
            with(extension.resources)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))
            archiveClassifier.set(classifier)
        }
    }

    fun setupDebugging(project: Project, zipProvider: Provider<RegularFile>) {
        val prepareHivemqHomeTask = project.tasks.register<PrepareHivemqHome>(PREPARE_HIVEMQ_HOME_TASK_NAME) {
            group = GROUP_NAME
            description =
                "Prepares a HiveMQ home directory with the HiveMQ extension for debugging via $RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME."

            hivemqExtensionZip.set(zipProvider)
            into(project.layout.buildDirectory.dir(HIVEMQ_HOME_FOLDER_NAME))
        }

        project.tasks.register<RunHivemq>(RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME) {
            group = GROUP_NAME
            description = "Runs HiveMQ with the extension for debugging."

            hivemqHomeDirectory.set(project.layout.dir(prepareHivemqHomeTask.map { it.destinationDir }))
        }
    }

    fun setupIntegrationTesting(project: Project, zipProvider: Provider<RegularFile>) {
        val sourceSets = project.the<SourceSetContainer>()
        val mainSourceSet = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
        val testSourceSet = sourceSets[SourceSet.TEST_SOURCE_SET_NAME]
        val integrationTestSourceSet = sourceSets.create(INTEGRATION_TEST_SOURCE_SET_NAME) {
            compileClasspath += mainSourceSet.output
            runtimeClasspath += mainSourceSet.output
        }

        project.configurations.named(integrationTestSourceSet.implementationConfigurationName) {
            extendsFrom(project.configurations[testSourceSet.implementationConfigurationName])
        }
        project.configurations.named(integrationTestSourceSet.runtimeOnlyConfigurationName) {
            extendsFrom(project.configurations[testSourceSet.runtimeOnlyConfigurationName])
        }

        val prepareTask = project.tasks.register<PrepareHivemqExtensionTest>(PREPARE_HIVEMQ_EXTENSION_TEST_TASK_NAME) {
            group = GROUP_NAME
            description = "Prepares the HiveMQ extension for integration testing."

            hivemqExtensionZip.set(zipProvider)
            into(project.layout.buildDirectory.dir(HIVEMQ_EXTENSION_TEST_FOLDER_NAME))
        }

        val integrationTestTask = project.tasks.register<Test>(INTEGRATION_TEST_TASK_NAME) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs integration tests."

            testClassesDirs = integrationTestSourceSet.output.classesDirs
            classpath = integrationTestSourceSet.runtimeClasspath + prepareTask.get().outputs.files
            shouldRunAfter(project.tasks.named(JavaPlugin.TEST_TASK_NAME))
        }

        project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME) { dependsOn(integrationTestTask) }
    }
}