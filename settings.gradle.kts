pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "${extra["plugin.kotlin.version"]}"
        id("com.gradle.plugin-publish") version "${extra["plugin.plugin-publish.version"]}"
        id("com.github.sgtsilvio.gradle.metadata") version "${extra["plugin.metadata.version"]}"
    }
}

rootProject.name = "hivemq-extension-gradle-plugin"