# HiveMQ Extension Gradle Plugin

[![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=brightgreen&label=gradle%20plugin&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fhivemq%2Fextension%2Fcom.hivemq.extension.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.hivemq.extension)

This Gradle plugin eases the development of HiveMQ extensions.

## Example with Groovy DSL

Content of the `settings.gradle` file:
```groovy
pluginManagement {
    plugins {
        id 'com.hivemq.extension' version '1.0.0'
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
    name.set('Example Extension')
    author.set('Example Org')
    priority.set(0)
    startPriority.set(1000)
    mainClass.set('org.example.ExtensionMain')
    sdkVersion.set('4.6.1')
}
```

## Example with Kotlin DSL

Content of the `settings.gradle.kts` file:
```kotlin
rootProject.name = "example-extension"

pluginManagement {
    plugins {
        id("com.hivemq.extension") version "1.0.0"
    }
}
```

Contents of the `build.gradle.kts` file:
```kotlin
plugins {
    id("com.hivemq.extension")
}

group = "org.example"
version = "1.0.0"

hivemqExtension {
    name.set("Example Extension")
    author.set("Example Org")
    priority.set(0)
    startPriority.set(1000)
    mainClass.set("org.example.ExtensionMain")
    sdkVersion.set("4.6.1")
}
```

## Tasks

### Build Tasks

| Task                               | Description |
|------------------------------------|-------------|
| `hivemqExtensionJar`               | Assembles the jar archive of the HiveMQ extension |
| `hivemqExtensionServiceDescriptor` | Generates the service descriptor of the HiveMQ extension |
| `hivemqExtensionXml`               | Generates the xml descriptor of the HiveMQ extension |
| `hivemqExtensionZip`               | Assembles the zip distribution of the HiveMQ extension |

### Run/Debug Tasks

| Task                     | Description |
|--------------------------|-------------|
| `prepareHivemqHome`      | Prepares a HiveMQ home directory with the HiveMQ extension for debugging via `runHivemqWithExtension` |
| `runHivemqWithExtension` | Runs HiveMQ with the extension for debugging |

### Test Tasks

| Task                   | Description |
|------------------------|-------------|
| `integrationTest`      | Runs integration tests, which can use the built extension under `build/hivemq-extension-test` |
| `prepareExtensionTest` | Prepares the HiveMQ extension for integration testing via `integrationTest` |

## Requirements

- Gradle 6.x or higher is required
- JDK 11 or higher is required
- Do not create descriptor files by yourself (`hivemq-extension.xml` or `com.hivemq.extension.sdk.api.ExtensionMain`).
  They are automatically generated.
- Do not add the `hivemq-extension-sdk` dependency yourself. It is added automatically with the right scopes.

## Build

Execute the `hivemqExtensionZip` task to build your extension.

You can find the output in `build/hivemq-extension` as `<project.name>-<project.version>.zip`

## Custom Resources

You can add custom resources to the extension zip distribution by putting files into the `src/hivemq-extension` directory.

Additionally, you can use the `hivemqExtension.resources` to add custom resources from any location or Gradle task.
`hivemqExtension.resources` is of type `CopySpec`, so you can use `from`, `exclude`, `include`, `rename`, etc.
(for a detailed explanation see the [Gradle documentation](https://docs.gradle.org/current/userguide/working_with_files.html))

Example:

```kotlin
hivemqExtension.resources {
    from("LICENSE")
    from("README.md") { rename { "README.txt" } }
}
```

## Run and Debug

Use the `prepareHivemqHome` task to define the contents of the HiveMQ home directory.

It is mandatory to set the `hivemqHomeDirectory` property to the path of a HiveMQ home directory (unzipped).
The contents of the HiveMQ home directory are copied to `build/hivemq-home`.

Your extension is built via the `hivemqExtensionZip` task and added automatically to `build/hivemq-home/extensions`.
It is also possible to specify a custom zip via the `hivemqExtensionZip` property.

`prepareHivemqHome` is of type `Copy`/`Sync`, so you can add any additional files (configs, licenses, other extensions, etc.).
(for a detailed explanation see the [Gradle documentation](https://docs.gradle.org/current/userguide/working_with_files.html))

The resulting home folder can be seen in `build/hivemq-home`.

Example:

```kotlin
tasks.prepareHivemqHome {
    hivemqHomeDirectory.set(file("/path/to/a/hivemq/folder")) // the only mandatory property
    from("config.xml") { into("conf") }
    from("src/test/resources/other-extension") { into("extensions") }
}
```

Execute the `runHivemqWithExtension` task to run HiveMQ with your extension from the configured home directory.

`runHivemqWithExtension` is of type `JavaExec`, so you can easily set debug options, system properties, JVM arguments, etc.

Example:

```kotlin
tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(true)
    }
}
```

## Integration Testing

This plugin adds an `integrationTest` task which executes tests from the `integrationTest` source set.
- Integration test source files are defined in `src/integrationTest`.
- Integration test dependencies are defined via the `integrationTestImplementation`, `integrationTestRuntimeOnly`, etc. configurations.

The `integrationTest` task builds the extension first and unzips it to the `build/hivemq-extension-test` directory.
The tests can then load the built extension into a [HiveMQ Test Container](https://github.com/hivemq/hivemq-testcontainer).