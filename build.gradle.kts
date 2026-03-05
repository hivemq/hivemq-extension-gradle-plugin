plugins {
    `kotlin-dsl`
    signing
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.spotless)
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

kotlin {
    jvmToolchain(21)
}

tasks.compileJava {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks.compileKotlin {
    kotlinJavaToolchain.toolchain.use(javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(11)
    })
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

@Suppress("UnstableApiUsage")
testing {
    suites {
        "test"(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter)
            dependencies {
                implementation(libs.assertj)
            }
        }
    }
}

spotless {
    kotlin {
        licenseHeaderFile(rootDir.resolve("HEADER"))
    }
}
