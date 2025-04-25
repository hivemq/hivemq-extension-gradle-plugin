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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.testing.base.TestingExtension
import org.gradle.util.GradleVersion

/**
 * @author Lukas Brand, Silvio Giebl
 */
@Suppress("unused")
class HivemqExtensionPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME: String = "hivemqExtension"
        const val TASK_GROUP_NAME: String = "hivemq extension"
        const val BUILD_FOLDER_NAME: String = "hivemq-extension"
        const val TASK_PREFIX: String = "hivemqExtension"
        const val JAR_SUFFIX: String = "Jar"
        private const val ZIP_SUFFIX: String = "Zip"
        private const val SERVICE_DESCRIPTOR_TASK_NAME: String = "${TASK_PREFIX}ServiceDescriptor"
        private const val XML_TASK_NAME: String = "${TASK_PREFIX}Xml"
        const val PROVIDED_CONFIGURATION_NAME: String = "hivemqProvided"

        private const val PREPARE_HIVEMQ_HOME_TASK_NAME: String = "prepareHivemqHome"
        private const val RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME: String = "runHivemqWithExtension"
        private const val HIVEMQ_HOME_FOLDER_NAME: String = "hivemq-home"

        private const val INTEGRATION_TEST_SUITE_NAME = "integrationTest"
        const val PREPARE_HIVEMQ_EXTENSION_TEST_TASK_NAME = "prepareHivemqExtensionTest"
        private const val HIVEMQ_EXTENSION_TEST_FOLDER_NAME = "hivemq-extension-test"
    }

    override fun apply(project: Project) {
        configureJava(project)
        val extension = createExtension(project)
        addDependencies(project, extension)
        val jarTask = registerJarTask(project, extension)
        registerXmlTask(project, extension)
        val zipTask = registerZipTask(project, extension, jarTask.archiveFile)

        setupDebugging(project, zipTask.archiveFile)
        setupIntegrationTesting(project, zipTask.archiveFile)
    }

    fun configureJava(project: Project) {
        project.plugins.apply(JavaPlugin::class)
        project.extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    fun createExtension(project: Project): HivemqExtensionExtension {
        return project.extensions.create(
            HivemqExtensionExtension::class,
            EXTENSION_NAME,
            HivemqExtensionExtensionImpl::class,
            { project.copySpec() },
        ).apply {
            mainClass.convention(project.providers.of(HivemqExtensionMainClassSource::class) {
                val mainSourceSet = project.extensions.getByType<SourceSetContainer>()[SourceSet.MAIN_SOURCE_SET_NAME]
                parameters.sources.from(mainSourceSet.allSource.sourceDirectories)
            })
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
        project.afterEvaluate {
            if (project.repositories.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
                project.repositories.mavenCentral()
            }
        }
    }

    private fun addDependencies(project: Project, extension: HivemqExtensionExtension) {
        addConfiguration(project).dependencies.addLater(extension.sdkVersion.map {
            project.dependencies.create("com.hivemq:hivemq-extension-sdk:$it")
        })
        addRepositories(project)
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

        return project.tasks.register<ShadowJar>(TASK_PREFIX + classifier.firstUppercase() + JAR_SUFFIX) {
            group = TASK_GROUP_NAME
            description =
                "Assembles the ${if (classifier.isEmpty()) "" else "$classifier "}jar of the HiveMQ extension."
            archiveClassifier.set(classifier)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))

            manifest.inheritFrom(project.tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME).get().manifest)
            from(project.extensions.getByType<SourceSetContainer>()[SourceSet.MAIN_SOURCE_SET_NAME].output)
            configurations = listOf(project.configurations[JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME])
            val providedConfiguration = project.configurations[PROVIDED_CONFIGURATION_NAME]
            for (component in providedConfiguration.incoming.resolutionResult.allComponents) {
                val id = component.moduleVersion
                if (id != null) {
                    dependencyFilter.exclude(dependencyFilter.dependency("${id.group}:${id.name}"))
                }
            }
            exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
            mergeServiceFiles()
        }
    }

    private fun registerServiceDescriptorTask(
        project: Project,
        extension: HivemqExtensionExtension,
    ): TaskProvider<HivemqExtensionServiceDescriptor> {
        return project.tasks.register<HivemqExtensionServiceDescriptor>(SERVICE_DESCRIPTOR_TASK_NAME) {
            group = TASK_GROUP_NAME
            description = "Generates the service descriptor of the HiveMQ extension."
            mainClass.set(extension.mainClass)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))
        }
    }

    fun registerXmlTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<HivemqExtensionXml> {
        return project.tasks.register<HivemqExtensionXml>(XML_TASK_NAME) {
            group = TASK_GROUP_NAME
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
        return project.tasks.register<HivemqExtensionZip>(TASK_PREFIX + classifier.firstUppercase() + ZIP_SUFFIX) {
            group = TASK_GROUP_NAME
            description =
                "Assembles the zip distribution of the HiveMQ extension${if (classifier.isEmpty()) "" else " containing the $classifier jar"}."
            archiveClassifier.set(classifier)
            destinationDirectory.set(project.layout.buildDirectory.dir(BUILD_FOLDER_NAME))
            jar.set(jarProvider)
            with(extension.resources)
        }
    }

    fun setupDebugging(project: Project, zipProvider: Provider<RegularFile>) {
        val prepareHivemqHomeTask = project.tasks.register<PrepareHivemqHome>(PREPARE_HIVEMQ_HOME_TASK_NAME) {
            group = TASK_GROUP_NAME
            description =
                "Prepares a HiveMQ home directory with the HiveMQ extension for debugging via $RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME."
            hivemqExtensionZip.set(zipProvider)
            into(project.layout.buildDirectory.dir(HIVEMQ_HOME_FOLDER_NAME))
        }

        project.tasks.register<RunHivemq>(RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME) {
            group = TASK_GROUP_NAME
            description = "Runs HiveMQ with the extension for debugging."
            hivemqHomeDirectory.set(project.layout.dir(prepareHivemqHomeTask.map { it.destinationDir }))
        }
    }

    @Suppress("UnstableApiUsage")
    fun setupIntegrationTesting(project: Project, zipProvider: Provider<RegularFile>) {
        val prepareTask = project.tasks.register<PrepareHivemqExtensionTest>(PREPARE_HIVEMQ_EXTENSION_TEST_TASK_NAME) {
            group = TASK_GROUP_NAME
            description = "Prepares the HiveMQ extension for integration testing."
            hivemqExtensionZip.set(zipProvider)
            into(project.layout.buildDirectory.dir(HIVEMQ_EXTENSION_TEST_FOLDER_NAME))
        }

        if (GradleVersion.current() >= GradleVersion.version("7.3")) {
            val testSuites = project.extensions.getByType<TestingExtension>().suites
            val integrationTestSuite = testSuites.register<JvmTestSuite>(INTEGRATION_TEST_SUITE_NAME) {
                targets.configureEach {
                    testTask {
                        classpath += prepareTask.get().outputs.files
                        shouldRunAfter(testSuites.named(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME))
                    }
                }
            }
            project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME) { dependsOn(integrationTestSuite) }
        } else {
            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            val integrationTestSourceSet = sourceSets.create(INTEGRATION_TEST_SUITE_NAME)
            val integrationTestTask = project.tasks.register<Test>(INTEGRATION_TEST_SUITE_NAME) {
                group = JavaBasePlugin.VERIFICATION_GROUP
                description = "Runs integration tests."
                testClassesDirs = integrationTestSourceSet.output.classesDirs
                classpath = integrationTestSourceSet.runtimeClasspath + prepareTask.get().outputs.files
                shouldRunAfter(project.tasks.named(JavaPlugin.TEST_TASK_NAME))
            }
            project.tasks.named(JavaBasePlugin.CHECK_TASK_NAME) { dependsOn(integrationTestTask) }
        }
    }
}