plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish")
    id("maven-publish")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.metadata")
}

group = "com.hivemq"
description = "A gradle plugin to ease the development of HiveMQ extensions"

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
        developer {
            id.set("ltb")
            name.set("Lukas Brand")
            email.set("lukas.brand@hivemq.com")
        }
        developer {
            id.set("SgtSilvio")
            name.set("Silvio Giebl")
            email.set("silvio.giebl@hivemq.com")
        }
    }
    github {
        org.set("hivemq")
        repo.set("hivemq-extension-gradle-plugin")
        issues()
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api("com.github.jengelman.gradle.plugins:shadow:${property("shadow.version")}")
}

gradlePlugin {
    plugins {
        create("extension") {
            id = "$group.$name"
            displayName = metadata.readableName.get()
            description = project.description
            implementationClass = "$group.extension.gradle.HivemqExtensionPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/hivemq/hivemq-extension-gradle-plugin"
    vcsUrl = "https://github.com/hivemq/hivemq-extension-gradle-plugin.git"
    tags = listOf("hivemq", "extension")
}

license {
    header = rootDir.resolve("HEADER")
    mapping("kt", "SLASHSTAR_STYLE")
}