package com.hivemq.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.*
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar

/**
 * @author Lukas Brand
 * @author Silvio Giebl
 */
class HivemqExtensionPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME: String = "hivemqExtension"
        const val GROUP_NAME: String = "hivemq extension"
        const val BUILD_FOLDER_NAME: String = "hivemq-extension"
        const val TASK_PREFIX: String = "hivemqExtension"
        const val JAR_SUFFIX: String = "jar"
        const val XML_SUFFIX: String = "xml"
        const val RESOURCES_SUFFIX: String = "resources"
        const val ZIP_SUFFIX: String = "zip"
        const val SERVICE_DESCRIPTOR_SUFFIX: String = "serviceDescriptor"

        fun taskName(suffix: String): String = TASK_PREFIX + suffix.capitalize()

        const val PREPARE_HIVEMQ_HOME_TASK_NAME: String = "prepareHivemqHome"
        const val RUN_HIVEMQ_WITH_EXTENSION_TASK_NAME: String = "runHivemqWithExtension"
        const val HIVEMQ_HOME_PROPERTY: String = "hivemq.home"
        const val HOME_FOLDER_NAME: String = "hivemq-home"
        const val EXTENSIONS_FOLDER_NAME: String = "extensions"
    }

    override fun apply(project: Project) {

        val extension = project.extensions.create(
            HivemqExtensionExtension::class.java,
            EXTENSION_NAME,
            HivemqExtensionExtensionImpl::class.java
        )

        configureJava(project)
        addDependencies(project, extension)
        val jarTask = registerJarTask(project, extension)
        val resourcesTask = registerResourcesTask(project, extension)
        val zipTask = registerZipTask(project, jarTask, resourcesTask)
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

    private fun addDependencies(project: Project, extension: HivemqExtensionExtension) {
        project.afterEvaluate {
            if (project.repositories.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
                project.repositories.mavenCentral()
            }
            val sdkDependency = "com.hivemq:hivemq-extension-sdk:${extension.sdkVersion}"
            project.dependencies.add("compileOnly", sdkDependency)
            project.dependencies.add("testImplementation", sdkDependency)
        }
    }

    fun registerJarTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<ShadowJar> {
        val serviceDescriptorTask = registerServiceDescriptorTask(project, extension)

        return project.tasks.register(taskName(JAR_SUFFIX), ShadowJar::class.java) {
            it.group = GROUP_NAME
            it.description = "Assembles the jar archive of the HiveMQ extension"

            it.destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME))

            it.manifest.inheritFrom((project.tasks.getByName("jar") as Jar).manifest)
            val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
            it.from(convention.sourceSets.getByName("main").output)
            it.configurations = listOf(project.configurations.getByName("runtimeClasspath"))
            it.exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
            it.from(serviceDescriptorTask) { copySpec -> copySpec.into("META-INF/services") }
        }
    }

    private fun registerServiceDescriptorTask(
        project: Project,
        extension: HivemqExtensionExtension
    ): TaskProvider<Task> {

        return project.tasks.register(taskName(SERVICE_DESCRIPTOR_SUFFIX)) {
            it.group = GROUP_NAME
            it.description = "Generates the service descriptor of the HiveMQ extension"

            it.inputs.property("mainClass", { extension.mainClass })

            val descriptorFile =
                project.buildDir.resolve(BUILD_FOLDER_NAME).resolve("com.hivemq.extension.sdk.api.ExtensionMain")
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

        return project.tasks.register(taskName(RESOURCES_SUFFIX), Sync::class.java) {
            it.group = GROUP_NAME
            it.description = "Collects the resources of the HiveMQ extension"

            it.from(xmlTask)
            it.into(project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(RESOURCES_SUFFIX))
        }
    }

    private fun registerXmlTask(project: Project, extension: HivemqExtensionExtension): TaskProvider<Task> {
        return project.tasks.register(taskName(XML_SUFFIX)) {
            it.group = GROUP_NAME
            it.description = "Generates the xml descriptor of the HiveMQ extension"

            it.inputs.property("id", { project.name })
            it.inputs.property("version", { project.version })
            it.inputs.property("name", { extension.name })
            it.inputs.property("author", { extension.author })
            it.inputs.property("priority", { extension.priority })
            it.inputs.property("start-priority", { extension.startPriority })

            val xmlFile = project.buildDir.resolve(BUILD_FOLDER_NAME).resolve("hivemq-extension.xml")
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
        resourcesTask: TaskProvider<Sync>
    ): TaskProvider<Zip> {

        val specialName = jarTask.name.removePrefix(TASK_PREFIX).removeSuffix(JAR_SUFFIX.capitalize())

        return project.tasks.register(taskName(specialName + ZIP_SUFFIX.capitalize()), Zip::class.java) {
            it.group = GROUP_NAME
            it.description = "Assembles the zip distribution of the HiveMQ extension"

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
            val hivemqHome = prepareHivemqHomeTask.get().outputs.files.asPath
            it.classpath("$hivemqHome/bin/hivemq.jar")
            it.systemProperty(HIVEMQ_HOME_PROPERTY, hivemqHome)
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

            it.dependsOn(it.extensionZipTask)
            it.from(it.hivemqFolder) { copySpec ->
                copySpec.exclude("$EXTENSIONS_FOLDER_NAME/${project.name}")
            }
            it.from(it.extensionZipTask.map { zip -> project.zipTree(zip.outputs.files.singleFile) }) { copySpec ->
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
                    else -> throw GradleException("The custom jar task must either be a Task or String.")
                }
                registerZipTask(project, customJarTask, resourcesTask)
            }
        }
    }
}
