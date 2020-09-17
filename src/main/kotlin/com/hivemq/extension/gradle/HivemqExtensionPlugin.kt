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
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar

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
        const val ZIP_SUFFIX: String = "zip"
        const val SERVICE_DESCRIPTOR_SUFFIX: String = "serviceDescriptor"
        const val EXTENSION_MAIN_CLASS_NAME: String = "com.hivemq.extension.sdk.api.ExtensionMain"
        const val XML_SUFFIX: String = "xml"
        const val EXTENSION_XML_NAME: String = "hivemq-extension.xml"

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
            project.plugins.apply(JavaPlugin::class.java)
        }

        project.extensions.configure<JavaPluginExtension>("java") {
            it.sourceCompatibility = JavaVersion.VERSION_11
            it.targetCompatibility = JavaVersion.VERSION_11
        }
    }

    fun createExtension(project: Project): HivemqExtensionExtension {
        return project.extensions.create(
            HivemqExtensionExtension::class.java,
            EXTENSION_NAME,
            HivemqExtensionExtensionImpl::class.java
        )
    }

    fun addRepositories(project: Project) {
        if (project.repositories.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
            project.repositories.mavenCentral()
        }
    }

    private fun addDependencies(project: Project, extension: HivemqExtensionExtension) {
        project.afterEvaluate {
            addRepositories(project)
            val sdkDependency = "com.hivemq:hivemq-extension-sdk:${extension.sdkVersion}"
            project.dependencies.add("compileOnly", sdkDependency)
            project.dependencies.add("testImplementation", sdkDependency)
        }
    }

    fun registerJarTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<ShadowJar> {
        val serviceDescriptorTask = registerServiceDescriptorTask(project, extension)
        project.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, Copy::class.java) {
            it.from(serviceDescriptorTask) { copySpec -> copySpec.into("META-INF/services") }
        }

        return project.tasks.register(TASK_PREFIX + JAR_SUFFIX.capitalize(), ShadowJar::class.java) {
            it.group = GROUP_NAME
            it.description = "Assembles the jar archive of the HiveMQ extension"

            it.destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME))

            it.manifest.inheritFrom(project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar::class.java).get().manifest)
            val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
            it.from(javaPluginConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output)
            it.configurations =
                listOf(project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME))
            it.exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
        }
    }

    private fun registerServiceDescriptorTask(
        project: Project,
        extension: HivemqExtensionExtension
    ): TaskProvider<Task> {

        return project.tasks.register(TASK_PREFIX + SERVICE_DESCRIPTOR_SUFFIX.capitalize()) {
            it.group = GROUP_NAME
            it.description = "Generates the service descriptor of the HiveMQ extension"

            it.inputs.property("mainClass", { extension.mainClass })

            val descriptorFile =
                project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(EXTENSION_MAIN_CLASS_NAME)
            it.outputs.file(descriptorFile)

            it.doFirst {
                val mainClass = extension.mainClass ?: throw GradleException("$EXTENSION_NAME: mainClass is missing.")

                descriptorFile.parentFile.mkdirs()
                descriptorFile.writeText(mainClass)
            }
        }
    }

    fun registerResourcesTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<Sync> {
        val xmlTask = registerXmlTask(project, extension)

        return project.tasks.register(TASK_PREFIX + RESOURCES_SUFFIX.capitalize(), Sync::class.java) {
            it.group = GROUP_NAME
            it.description = "Collects the resources of the HiveMQ extension"

            it.from(xmlTask)
            it.into(project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(RESOURCES_SUFFIX))
        }
    }

    private fun registerXmlTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<Task> {
        return project.tasks.register(TASK_PREFIX + XML_SUFFIX.capitalize()) {
            it.group = GROUP_NAME
            it.description = "Generates the xml descriptor of the HiveMQ extension"

            it.inputs.property("id", { project.name })
            it.inputs.property("version", { project.version })
            it.inputs.property("name", { extension.name })
            it.inputs.property("author", { extension.author })
            it.inputs.property("priority", { extension.priority })
            it.inputs.property("start-priority", { extension.startPriority })

            val xmlFile = project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(EXTENSION_XML_NAME)
            it.outputs.file(xmlFile)

            it.doFirst {
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

        return project.tasks.register(
            taskPrefix + specialName.capitalize() + ZIP_SUFFIX.capitalize(),
            Zip::class.java
        ) {
            it.group = GROUP_NAME
            it.description = "Assembles the zip distribution of the HiveMQ extension" +
                    if (specialName.isEmpty()) "" else " containing the ${specialName.decapitalize()} jar"

            it.destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME))
            it.archiveClassifier.set(specialName.toLowerCase())

            it.from(jarTask) { copySpec -> copySpec.rename { "${project.name}-${project.version}.jar" } }
            it.from(resourcesTask)
            it.into(project.name)
        }
    }

    fun registerRunHivemqWithExtensionTask(project: Project, zipTask: TaskProvider<Zip>): TaskProvider<JavaExec> {
        val prepareHivemqHomeTask = registerPrepareHivemqHomeTask(project, zipTask)

        return project.tasks.register(RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME, JavaExec::class.java) {
            it.group = GROUP_NAME
            it.description = "Runs HiveMQ with the extension"

            it.dependsOn(prepareHivemqHomeTask)
            val hivemqHome = prepareHivemqHomeTask.get().destinationDir.path
            it.classpath("$hivemqHome/bin/hivemq.jar")
            it.systemProperty(HIVEMQ_HOME_PROPERTY_NAME, hivemqHome)
            it.jvmArgs("-Djava.net.preferIPv4Stack=true", "-noverify")
            it.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
            it.jvmArgs("--add-opens", "java.base/java.nio=ALL-UNNAMED")
            it.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
            it.jvmArgs("--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED")
            it.jvmArgs("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
        }
    }

    private fun registerPrepareHivemqHomeTask(
        project: Project,
        zipTask: TaskProvider<Zip>
    ): TaskProvider<PrepareHivemqHome> {

        return project.tasks.register(PREPARE_HIVEMQ_HOME_TASK_NAME, PrepareHivemqHome::class.java) {
            it.group = GROUP_NAME
            it.description = "Collects the resources of the HiveMQ home for $RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME"

            it.extensionZipTask.set(zipTask)

            it.from(it.hivemqFolder) { copySpec ->
                copySpec.exclude("$EXTENSIONS_FOLDER_NAME/${project.name}")
            }
            it.from(it.extensionZipTask.map { zip -> project.zipTree(zip.archiveFile) }) { copySpec ->
                copySpec.into(EXTENSIONS_FOLDER_NAME)
            }
            it.into(project.buildDir.resolve(HOME_FOLDER_NAME))
            it.duplicatesStrategy = DuplicatesStrategy.WARN
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
