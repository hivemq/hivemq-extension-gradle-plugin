plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish")
    id("com.github.hierynomus.license")
    id("io.github.sgtsilvio.gradle.defaults")
    id("io.github.sgtsilvio.gradle.metadata")
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
    api("com.github.jengelman.gradle.plugins:shadow:${property("shadow.version")}")
    constraints {
        //not used for logging, only PluginCache is used
        implementation("org.apache.logging.log4j:log4j-core:2.17.2")
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

license {
    header = rootDir.resolve("HEADER")
    mapping("kt", "SLASHSTAR_STYLE")
}
