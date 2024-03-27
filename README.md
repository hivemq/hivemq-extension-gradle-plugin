# HiveMQ Extension Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.hivemq.extension?color=brightgreen&style=for-the-badge)](https://plugins.gradle.org/plugin/com.hivemq.extension)
[![GitHub](https://img.shields.io/github/license/hivemq/hivemq-extension-gradle-plugin?color=brightgreen&style=for-the-badge)](LICENSE)

This Gradle plugin eases the development of HiveMQ extensions.

## Example with Groovy DSL

Contents of the `build.gradle` file:
```groovy
plugins {
    id 'com.hivemq.extension' version '3.1.0'
}

group = 'org.example'
version = '1.0.0'

hivemqExtension {
    name = 'Example Extension'
    author = 'Example Org'
    priority = 0
    startPriority = 1000
    mainClass = 'org.example.ExtensionMain'
    sdkVersion = '4.6.2'
}
```

## Example with Kotlin DSL

Contents of the `build.gradle.kts` file:
```kotlin
plugins {
    id("com.hivemq.extension") version "3.1.0"
}

group = "org.example"
version = "1.0.0"

hivemqExtension {
    name = "Example Extension"
    author = "Example Org"
    priority = 0
    startPriority = 1000
    mainClass = "org.example.ExtensionMain"
    sdkVersion = "4.6.2"
}
```

## Tasks

### Build Tasks

| Task                               | Description                                              |
|------------------------------------|----------------------------------------------------------|
| `hivemqExtensionJar`               | Assembles the jar of the HiveMQ extension                |
| `hivemqExtensionServiceDescriptor` | Generates the service descriptor of the HiveMQ extension |
| `hivemqExtensionXml`               | Generates the xml descriptor of the HiveMQ extension     |
| `hivemqExtensionZip`               | Assembles the zip distribution of the HiveMQ extension   |

### Run/Debug Tasks

| Task                     | Description                                                                                           |
|--------------------------|-------------------------------------------------------------------------------------------------------|
| `prepareHivemqHome`      | Prepares a HiveMQ home directory with the HiveMQ extension for debugging via `runHivemqWithExtension` |
| `runHivemqWithExtension` | Runs HiveMQ with the extension for debugging                                                          |

### Test Tasks

| Task                   | Description                                                                       |
|------------------------|-----------------------------------------------------------------------------------|
| `integrationTest`      | Runs integration tests, which can use the built extension as a classpath resource |
| `prepareExtensionTest` | Prepares the HiveMQ extension for integration testing via `integrationTest`       |

## Requirements

- Gradle 6.7 or higher is required (Gradle 8.x is recommended, special steps are required for Gradle 7.x and 6.x (see below) because of the compatibility requirements of the [Gradle Shadow plugin](https://github.com/johnrengelman/shadow))
- JDK 11 or higher is required
- Do not create descriptor files by yourself (`hivemq-extension.xml` or `com.hivemq.extension.sdk.api.ExtensionMain`).
  They are automatically generated.
- Do not add the `hivemq-extension-sdk` dependency yourself. It is added automatically with the right scopes.

### Running on Gradle 7.x

If you run on Gradle 7.x, please add the following to your `build.gradle(.kts)` in addition to applying this plugin:

```kotlin
buildscript {
    dependencies {
        classpath("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    }
    configurations.classpath {
        exclude("com.github.johnrengelman", "shadow")
    }
}
```

### Running on Gradle 6.x

If you run on Gradle 6.x, please add the following to your `build.gradle(.kts)` in addition to applying this plugin:

```kotlin
buildscript {
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    }
    configurations.classpath {
        exclude("com.github.johnrengelman", "shadow")
    }
}
```

## Build

Execute the `hivemqExtensionZip` task to build your extension.

You can find the output in `build/hivemq-extension` as `<project.name>-<project.version>.zip`

## Custom Resources

You can add custom resources to the extension zip distribution by putting files into the `src/hivemq-extension` directory.

Additionally, you can use `hivemqExtension.resources` to add custom resources from any location or Gradle task.
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

The resulting home directory can be seen in `build/hivemq-home`.

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
The tests can then load the built extension into a [HiveMQ Test Container](https://github.com/hivemq/hivemq-testcontainer) as a classpath resource.