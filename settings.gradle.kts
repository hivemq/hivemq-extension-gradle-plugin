rootProject.name = "hivemq-extension-gradle-plugin"

pluginManagement {
    plugins {
        id("com.gradle.plugin-publish") version "${extra["plugin.plugin-publish.version"]}"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("io.github.sgtsilvio.gradle.defaults") version "${extra["plugin.defaults.version"]}"
        id("io.github.sgtsilvio.gradle.metadata") version "${extra["plugin.metadata.version"]}"
    }
}