plugins {
    `kotlin-dsl`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.license)
}

group = "com.hivemq"
description = "A Gradle plugin to ease the development of HiveMQ extensions"

metadata {
    readableName.set("HiveMQ Extension Gradle Plugin")
    organization {
        name.set("HiveMQ and the HiveMQ Community")
        url.set("https://www.hivemq.com/")
    }
    license {
        apache2()
    }
    developers {
        register("ltb") {
            fullName.set("Lukas Brand")
            email.set("lukas.brand@hivemq.com")
        }
        register("SgtSilvio") {
            fullName.set("Silvio Giebl")
            email.set("silvio.giebl@hivemq.com")
        }
    }
    github {
        issues()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api(libs.shadow)
    constraints {
        //not used for logging, only PluginCache is used
        implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    }
}

gradlePlugin {
    website.set(metadata.url)
    vcsUrl.set(metadata.scm.get().url)
    plugins {
        create("extension") {
            id = "$group.$name"
            implementationClass = "$group.$name.gradle.HivemqExtensionPlugin"
            displayName = metadata.readableName.get()
            description = project.description
            tags.set(listOf("hivemq", "extension"))
        }
    }
}

val pluginTestRepository = publishing.repositories.maven {
    name = "pluginTest"
    url = uri(layout.buildDirectory.dir("pluginTest/repository"))
}

val pluginTestInitScript by tasks.registering {
    group = "Plugin development"
    description = "Generates the init script for plugin functional tests."

    val repositoryPath = file(pluginTestRepository.url).absolutePath
    inputs.property("repositoryPath", repositoryPath)
    val pluginIds = provider { gradlePlugin.plugins.map { it.id } }
    inputs.property("pluginIds", pluginIds)
    val coordinates = provider { (publishing.publications["pluginMaven"] as MavenPublication).run { "$groupId:$artifactId:$version" } }
    inputs.property("coordinates", coordinates)
    val outputFile = project.layout.buildDirectory.file("pluginTest/init.gradle.kts")
    outputs.file(outputFile)
    doLast {
        val pluginIdsString = pluginIds.get().joinToString("\", \"", "\"", "\"")
        outputFile.get().asFile.writeText(
            """
            beforeSettings {
                pluginManagement {
                    repositories {
                        maven { url = uri("$repositoryPath") }
                        gradlePluginPortal()
                    }
                    resolutionStrategy {
                        eachPlugin {
                            if ((requested.id.id in listOf($pluginIdsString)) && (requested.version == null)) {
                                useModule("${coordinates.get()}")
                            }
                        }
                    }
                }
            }
            """.trimIndent()
        )
    }
}

testing {
    suites.named<JvmTestSuite>("test") {
        useJUnitJupiter(libs.versions.junit.jupiter)
        targets.configureEach {
            testTask {
                dependsOn("publishPluginMavenPublicationToPluginTestRepository")
                inputs.dir(pluginTestRepository.url)
                inputs.file(pluginTestInitScript.map {it.outputs.files.singleFile })
                systemProperty("pluginTestInitScript", pluginTestInitScript.get().outputs.files.singleFile.absolutePath)
            }
        }
    }
}

afterEvaluate {
    tasks.named<PublishToMavenRepository>("publishPluginMavenPublicationToPluginTestRepository") {
        outputs.dir(repository.url)
        doFirst {
            delete(repository.url)
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    val predicate = provider { (repository.name != "pluginTest") || (publication.name == "pluginMaven") }
    onlyIf("only pluginMaven publication is published to pluginTest repository") { predicate.get() }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("kt", "SLASHSTAR_STYLE")
}
