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
import org.gradle.api.*
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
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
        const val JAR_SUFFIX: String = "jar"
        const val RESOURCES_SUFFIX: String = "resources"
        const val RESOURCES_FOLDER_PATH: String = "src/hivemq-extension"
        const val ZIP_SUFFIX: String = "zip"
        const val SERVICE_DESCRIPTOR_SUFFIX: String = "serviceDescriptor"
        const val EXTENSION_MAIN_CLASS_NAME: String = "com.hivemq.extension.sdk.api.ExtensionMain"
        const val XML_SUFFIX: String = "xml"
        const val EXTENSION_XML_NAME: String = "hivemq-extension.xml"
        const val PROVIDED_CONFIGURATION_NAME: String = "hivemqProvided"

        const val PREPARE_HIVEMQ_HOME_TASK_NAME: String = "prepareHivemqHome"
        const val RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME: String = "runHivemqWithExtension"
        const val HIVEMQ_HOME_PROPERTY_NAME: String = "hivemq.home"
        const val HOME_FOLDER_NAME: String = "hivemq-home"
        const val EXTENSIONS_FOLDER_NAME: String = "extensions"
    }

    override fun apply(project: Project) {
        configureJava(project)
        val extension = createExtension(project)
        addDependencies(project, extension)
        val jarTask = registerJarTask(project, extension)
        val resourcesTask = registerResourcesTask(project, extension)
        val zipTask = registerZipTask(project, jarTask, resourcesTask, TASK_PREFIX)
        registerRunHivemqWithExtensionTask(project, zipTask)

        registerCustomZipTask(project, extension, resourcesTask)
    }

    fun configureJava(project: Project) {
        if (!project.plugins.hasPlugin("java")) {
            project.plugins.apply(JavaPlugin::class)
        }

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
            HivemqExtensionExtensionImpl::class
        )
    }

    fun addConfiguration(project: Project): NamedDomainObjectProvider<Configuration> {
        val providedConfiguration = project.configurations.register(PROVIDED_CONFIGURATION_NAME) {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
        project.configurations.named(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME) {
            extendsFrom(providedConfiguration.get())
        }
        return providedConfiguration
    }

    fun addRepositories(project: Project) {
        if (project.repositories.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
            project.repositories.mavenCentral()
        }
    }

    private fun addDependencies(project: Project, extension: HivemqExtensionExtension) {
        addConfiguration(project)
        project.afterEvaluate {
            addRepositories(project)
            val sdkDependency = "com.hivemq:hivemq-extension-sdk:${extension.sdkVersion}"
            project.dependencies.add(PROVIDED_CONFIGURATION_NAME, sdkDependency)
        }
    }

    fun registerJarTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<ShadowJar> {
        val serviceDescriptorTask = registerServiceDescriptorTask(project, extension)
        project.tasks.named<Copy>(JavaPlugin.PROCESS_RESOURCES_TASK_NAME) {
            from(serviceDescriptorTask) { into("META-INF/services") }
        }

        return project.tasks.register<ShadowJar>(TASK_PREFIX + JAR_SUFFIX.capitalize()) {
            group = GROUP_NAME
            description = "Assembles the jar archive of the HiveMQ extension"

            destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME))

            manifest.inheritFrom(project.tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME).get().manifest)
            from(project.the<JavaPluginConvention>().sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].output)
            configurations = listOf(project.configurations[JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME])
            val providedConfiguration = project.configurations[PROVIDED_CONFIGURATION_NAME]
            for (artifact in providedConfiguration.resolvedConfiguration.resolvedArtifacts) {
                val id = artifact.id.componentIdentifier
                if (id is ModuleComponentIdentifier) {
                    dependencyFilter.exclude(dependencyFilter.dependency("${id.group}:${id.module}"))
                }
            }
            exclude(
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "module-info.class"
            )
        }
    }

    private fun registerServiceDescriptorTask(
        project: Project,
        extension: HivemqExtensionExtension
    ): TaskProvider<Task> {

        return project.tasks.register(TASK_PREFIX + SERVICE_DESCRIPTOR_SUFFIX.capitalize()) {
            group = GROUP_NAME
            description = "Generates the service descriptor of the HiveMQ extension"

            inputs.property("mainClass", { extension.mainClass ?: "" })

            val descriptorFile = project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(EXTENSION_MAIN_CLASS_NAME)
            outputs.file(descriptorFile)
            outputs.upToDateWhen { extension.mainClass != null }

            doLast {
                if (extension.mainClass == null) {
                    extension.mainClass = findMainClass(project)
                }
                val mainClass = extension.mainClass ?: throw GradleException("$EXTENSION_NAME: mainClass is missing.")

                descriptorFile.parentFile.mkdirs()
                descriptorFile.writeText(mainClass)
            }
        }
    }

    private fun findMainClass(project: Project): String? {
        val regex = Regex("[ ,:]ExtensionMain[ ,{]")
        var mainClass: String? = null
        project.the<JavaPluginConvention>().sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].allSource.visit {
            if (!isDirectory && (file.name.endsWith(".java") || file.name.endsWith(".kt")) &&
                file.readText().contains(regex)
            ) {
                mainClass = relativePath.pathString.substringBeforeLast('.').replace('/', '.')
                stopVisiting()
            }
        }
        return mainClass
    }

    fun registerResourcesTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<Sync> {
        val xmlTask = registerXmlTask(project, extension)

        return project.tasks.register<Sync>(TASK_PREFIX + RESOURCES_SUFFIX.capitalize()) {
            group = GROUP_NAME
            description = "Collects the resources of the HiveMQ extension"

            from(xmlTask)
            from(RESOURCES_FOLDER_PATH)
            into(project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(RESOURCES_SUFFIX))
        }
    }

    private fun registerXmlTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<Task> {
        return project.tasks.register(TASK_PREFIX + XML_SUFFIX.capitalize()) {
            group = GROUP_NAME
            description = "Generates the xml descriptor of the HiveMQ extension"

            inputs.property("id", { project.name })
            inputs.property("version", { project.version })
            inputs.property("name", { extension.name })
            inputs.property("author", { extension.author })
            inputs.property("priority", { extension.priority })
            inputs.property("start-priority", { extension.startPriority })

            val xmlFile = project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(EXTENSION_XML_NAME)
            outputs.file(xmlFile)

            doLast {
                val name = extension.name ?: throw GradleException("$EXTENSION_NAME: name is missing.")
                val author = extension.author ?: throw GradleException("$EXTENSION_NAME: author is missing.")

                xmlFile.parentFile.mkdirs()
                xmlFile.writeText(
                    """
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <hivemq-extension>
                            <id>${project.name}</id>
                            <version>${project.version}</version>
                            <name>$name</name>
                            <author>$author</author>
                            <priority>${extension.priority}</priority>
                            <start-priority>${extension.startPriority}</start-priority>
                        </hivemq-extension>
                    """.trimIndent()
                )
            }
        }
    }

    fun registerZipTask(
        project: Project,
        jarTask: TaskProvider<*>,
        resourcesTask: TaskProvider<Sync>,
        taskPrefix: String
    ): TaskProvider<Zip> {

        val specialName = jarTask.name.removePrefix(taskPrefix).removeSuffix(JAR_SUFFIX.capitalize())

        return project.tasks.register<Zip>(taskPrefix + specialName.capitalize() + ZIP_SUFFIX.capitalize()) {
            group = GROUP_NAME
            description = "Assembles the zip distribution of the HiveMQ extension" +
                    if (specialName.isEmpty()) "" else " containing the ${specialName.decapitalize()} jar"

            destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME))
            archiveClassifier.set(specialName.toLowerCase())

            from(jarTask) { rename { "${project.name}-${project.version}.jar" } }
            from(resourcesTask)
            into(project.name)
        }
    }

    fun registerRunHivemqWithExtensionTask(project: Project, zipTask: TaskProvider<Zip>): TaskProvider<JavaExec> {
        val prepareHivemqHomeTask = registerPrepareHivemqHomeTask(project, zipTask)

        return project.tasks.register<JavaExec>(RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME) {
            group = GROUP_NAME
            description = "Runs HiveMQ with the extension"

            dependsOn(prepareHivemqHomeTask)
            val hivemqHome = prepareHivemqHomeTask.get().destinationDir.path
            classpath("$hivemqHome/bin/hivemq.jar")
            systemProperty(HIVEMQ_HOME_PROPERTY_NAME, hivemqHome)
            jvmArgs("-Djava.net.preferIPv4Stack=true", "-noverify")
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.base/java.nio=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
            jvmArgs("--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED")
            jvmArgs("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
        }
    }

    private fun registerPrepareHivemqHomeTask(
        project: Project,
        zipTask: TaskProvider<Zip>
    ): TaskProvider<PrepareHivemqHome> {

        return project.tasks.register<PrepareHivemqHome>(PREPARE_HIVEMQ_HOME_TASK_NAME) {
            group = GROUP_NAME
            description = "Collects the resources of the HiveMQ home for $RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME"

            extensionZipTask.set(zipTask)

            from(hivemqFolder) { exclude("$EXTENSIONS_FOLDER_NAME/${project.name}") }
            from(extensionZipTask.map { zip -> project.zipTree(zip.archiveFile) }) { into(EXTENSIONS_FOLDER_NAME) }
            into(project.buildDir.resolve(HOME_FOLDER_NAME))
            duplicatesStrategy = DuplicatesStrategy.WARN

            doFirst {
                if (!project.file(hivemqFolder.get()).exists()) {
                    throw GradleException("hivemqFolder ${hivemqFolder.get()} does not exist")
                }
            }
        }
    }

    private fun registerCustomZipTask(
        project: Project,
        extension: HivemqExtensionExtension,
        resourcesTask: TaskProvider<Sync>
    ) {
        project.afterEvaluate {
            val customJarTaskAny = extension.customJarTask
            if (customJarTaskAny != null) {
                val customJarTask = when (customJarTaskAny) {
                    is TaskProvider<*> -> customJarTaskAny
                    is String -> project.tasks.named(customJarTaskAny)
                    else -> throw GradleException("The custom jar task must either be a TaskProvider or String.")
                }
                registerZipTask(project, customJarTask, resourcesTask, TASK_PREFIX)
            }
        }
    }
}
