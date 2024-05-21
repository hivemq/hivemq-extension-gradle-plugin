plugins {
    `kotlin-dsl`
    signing
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.license)
}

group = "com.hivemq"

metadata {
    readableName = "HiveMQ Extension Gradle Plugin"
    description = "A Gradle plugin to ease the development of HiveMQ extensions"
    organization {
        name = "HiveMQ and the HiveMQ Community"
        url = "https://www.hivemq.com/"
    }
    license {
        apache2()
    }
    developers {
        register("LukasBrand") {
            fullName = "Lukas Brand"
            email = "lukas.brand@hivemq.com"
        }
        register("SgtSilvio") {
            fullName = "Silvio Giebl"
            email = "silvio.giebl@hivemq.com"
        }
    }
    github {
        issues()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api(libs.shadow)
}

gradlePlugin {
    plugins {
        create("extension") {
            id = "$group.$name"
            implementationClass = "$group.$name.gradle.HivemqExtensionPlugin"
            tags = listOf("hivemq", "extension")
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
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
    val coordinates =
        provider { (publishing.publications["pluginMaven"] as MavenPublication).run { "$groupId:$artifactId:$version" } }
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

@Suppress("UnstableApiUsage")
testing {
    suites {
        "test"(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter)
            targets.configureEach {
                testTask {
                    dependsOn("publishPluginMavenPublicationToPluginTestRepository")
                    inputs.dir(pluginTestRepository.url)
                    inputs.file(pluginTestInitScript.map { it.outputs.files.singleFile })
                    systemProperty(
                        "pluginTestInitScript",
                        pluginTestInitScript.get().outputs.files.singleFile.absolutePath,
                    )
                }
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
