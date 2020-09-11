package com.hivemq.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.*
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar

class HiveMqExtensionPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME: String = "hivemqExtension"
        const val GROUP_NAME: String = "hivemqExtension"
        const val BUILD_FOLDER_NAME: String = "hivemq-extension"
        const val JAR_SUFFIX: String = "jar"
        const val XML_SUFFIX: String = "xml"
        const val RESOURCES_SUFFIX: String = "resources"
        const val ZIP_SUFFIX: String = "zip"

        fun taskName(suffix: String): String = GROUP_NAME + suffix.capitalize()
    }

    override fun apply(project: Project) {

        val extension = project.extensions.create(
            HiveMqExtensionExtension::class.java,
            EXTENSION_NAME,
            HiveMqExtensionExtensionImpl::class.java
        )

        configureJava(project)
        addDependencies(project, extension)
        val jarTask = registerJarTask(project)
        val xmlTask = registerXmlTask(project, extension)
        val resourcesTask = registerResourcesTask(project, xmlTask)
        registerZipTask(project, jarTask, resourcesTask)

//        project.afterEvaluate {
//            val customJarTaskName = extension.customJarTask
//            if (customJarTaskName != null) {
//                if (project.tasks.findByName(customJarTaskName) == null) {
//                    throw GradleException("The custom jar task \"${customJarTaskName}\" does not exist.")
//                }
//                val customJarTask = project.tasks.named(customJarTaskName) {
//                    it.dependsOn(jarTask)
//                    it.inputs.file(jarTask.get().outputs.files.singleFile)
//                }
//
//                registerZipTask(project, customJarTask, resourcesTask)
//            }
//        }
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

    private fun addDependencies(project: Project, extension: HiveMqExtensionExtension) {
        project.afterEvaluate {
            if (project.repositories.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
                project.repositories.mavenCentral()
            }
            val sdkDependency = "com.hivemq:hivemq-extension-sdk:${extension.sdkVersion ?: "latest.integration"}"
            project.dependencies.add("compileOnly", sdkDependency)
            project.dependencies.add("testImplementation", sdkDependency)
        }
    }

    fun registerJarTask(project: Project): TaskProvider<ShadowJar> {
        project.plugins.apply(ShadowPlugin::class.java)

        return project.tasks.register(taskName(JAR_SUFFIX), ShadowJar::class.java) {
            it.group = GROUP_NAME
            it.description = "Assembles the jar archive of the HiveMQ extension"

            it.destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(JAR_SUFFIX))

            it.manifest.inheritFrom((project.tasks.getByName("jar") as Jar).manifest)
            val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
            it.from(convention.sourceSets.getByName("main").output)
            it.configurations = listOf(project.configurations.getByName("runtimeClasspath"))
            it.exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
        }
    }

    fun registerXmlTask(project: Project, extension: HiveMqExtensionExtension): TaskProvider<Task> {
        return project.tasks.register(taskName(XML_SUFFIX)) {
            it.group = GROUP_NAME
            it.description = "Generates the xml descriptor of the HiveMQ extension"

            it.inputs.property("id", { project.name })
            it.inputs.property("name", { extension.name })
            it.inputs.property("version", { project.version })
            it.inputs.property("author", { extension.author })
            it.inputs.property("priority", { extension.priority })
            it.inputs.property("start-priority", { extension.startPriority })

            val xmlFile = project.buildDir.resolve(BUILD_FOLDER_NAME).resolve("hivemq-extension.xml")
            it.outputs.file(xmlFile)

            it.doFirst {
                val name =
                    extension.name ?: throw GradleException("hivemqExtension: extensionName attribute is missing.")
                val author =
                    extension.author ?: throw GradleException("hivemqExtension: extensionAuthor attribute is missing.")
                val priority = extension.priority ?: 1_000
                val startPriority = extension.startPriority ?: 1_000

                xmlFile.parentFile.mkdirs()
                xmlFile.writeText(
                    """
                        <hivemq-extension>
                            <id>${project.name}</id>
                            <name>${name}</name>
                            <version>${project.version}</version>
                            <author>${author}</author>
                            <priority>${priority}</priority>
                            <start-priority>${startPriority}</start-priority>
                        </hivemq-extension>
                    """.trimIndent()
                )
            }
        }
    }

    fun registerResourcesTask(project: Project, xmlTask: TaskProvider<Task>): TaskProvider<Copy> {
        return project.tasks.register(taskName(RESOURCES_SUFFIX), Copy::class.java) {
            it.group = GROUP_NAME
            it.description = "Collects the resources of the HiveMQ extension"

            it.from(xmlTask)
            it.into(project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(RESOURCES_SUFFIX))
        }
    }

    fun registerZipTask(
        project: Project,
        jarTask: TaskProvider<out Jar>,
        resourcesTask: TaskProvider<Copy>
    ): TaskProvider<Zip> {

        val specialName = jarTask.name.removePrefix(GROUP_NAME).removeSuffix(JAR_SUFFIX.capitalize())

        return project.tasks.register(taskName(specialName + ZIP_SUFFIX.capitalize()), Zip::class.java) {
            it.group = GROUP_NAME
            it.description = "Assembles the zip distribution of the HiveMQ extension"

            it.destinationDirectory.set(project.buildDir.resolve(BUILD_FOLDER_NAME).resolve(ZIP_SUFFIX))

            it.from(jarTask) { copySpec -> copySpec.rename { "${project.name}-${project.version}.jar" } }
            it.from(resourcesTask)
            it.into(project.name)
        }
    }
}
