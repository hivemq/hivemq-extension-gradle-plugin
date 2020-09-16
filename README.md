# HiveMQ Extension Gradle Plugin

[![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=brightgreen&label=gradle%20plugin&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fhivemq%2Fextension%2Fcom.hivemq.extension.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.hivemq.extension)

This gradle plugin eases the development of HiveMQ extensions.

## Example with Groovy DSL

Content of the `settings.gradle` file:
```groovy
pluginManagement {
    plugins {
        id 'com.hivemq.extension' version '0.1.0'
    }
}

rootProject.name = 'example-extension'
```

Contents of the `build.gradle` file:
```groovy
plugins {
    id 'com.hivemq.extension'
}

group 'org.example'
version '1.0.0'

hivemqExtension {
    name = 'Example Extension'
    author = 'Example Org'
    priority = 0
    startPriority = 1000
    mainClass = 'org.example.ExtensionMain'
    sdkVersion = '4.4.1'
}
```

## Example with Kotlin DSL

Content of the `settings.gradle.kts` file:
```kotlin
pluginManagement {
    plugins {
        id("com.hivemq.extension") version "0.1.0"
    }
}

rootProject.name = "example-extension"
```

Contents of the `build.gradle.kts` file:
```kotlin
plugins {
    id("com.hivemq.extension")
}

group = "org.example"
version = "1.0.0"

hivemqExtension {
    name = "Example Extension"
    author = "Example Org"
    priority = 0
    startPriority = 1000
    mainClass = "org.example.ExtensionMain"
    sdkVersion = "4.4.1"
}
```

## Tasks

| Task                               | Description |
|------------------------------------|-------------|
| `hivemqExtensionJar`               | Assembles the jar archive of the HiveMQ extension |
| `hivemqExtensionResources`         | Collects the resources of the HiveMQ extension |
| `hivemqExtensionServiceDescriptor` | Generates the service descriptor of the HiveMQ extension |
| `hivemqExtensionXml`               | Generates the xml descriptor of the HiveMQ extension |
| `hivemqExtensionZip`               | Assembles the zip distribution of the HiveMQ extension |
| `prepareHivemqHome`                | Collects the resources of the HiveMQ home for `runHivemqWithExtension` |
| `runHivemqWithExtension`           | Runs HiveMQ with the extension |

## Requirements

- Gradle 6.x or higher is required
- Do not create descriptor files by yourself (`hivemq-extension.xml` or `com.hivemq.extension.sdk.api.ExtensionMain`).
  They are automatically generated.
- Do not add the `hivemq-extension-sdk` dependency yourself. It is added automatically with the right scopes.

## Build

Execute the `hivemqExtensionZip` task to build your extension.

You can find the output in `build/hivemq-extension` as `<project.name>-<project.version>.zip`

## Custom Resources

You can use the `hivemqExtensionResources` task to add custom resources to the extension zip.
As it is a `Copy`/`Sync` task, you can use `from`, `exclude`, `include`, `rename`, etc.
([gradle documentation](https://docs.gradle.org/current/userguide/working_with_files.html))

Example:

```kotlin
tasks.hivemqExtensionResources {
    from("LICENSE")
    from("README.md") { rename { "README.txt" } }
}
```

## Run and Debug

Use the `prepareHivemqHome` task to define the contents of the HiveMQ home folder.
Your extension is built and added automatically.

As it is a `Copy`/`Sync` task ([gradle documentation](https://docs.gradle.org/current/userguide/working_with_files.html)), 
you can add any files (configs, other extensions, etc.).
The resulting home folder can be seen in `build/hivemq-home`.

Example:

```kotlin
tasks.prepareHivemqHome {
    hivemqFolder.set("/path/to/a/hivemq/folder") // the only mandatory property
    from("config.xml") { into("conf") }
    from("src/test/resources/other-extension") { into("extensions") }
}
```

Execute the `runHivemqWithExtension` task to run HiveMQ with your extension from the configured home folder.

As it is a gradle `JavaExec` task, you can easily set debug options, system properties, JVM arguments, etc.

```kotlin
tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(true)
    }
}
```