package com.hivemq.extension.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * @author Silvio Giebl
 */
internal abstract class HivemqExtensionMainClassSource :
    ValueSource<String, HivemqExtensionMainClassSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val sources: ConfigurableFileCollection
    }

    final override fun obtain(): String? {
        val regex = Regex("[ ,:]ExtensionMain[ ,{]")
        var mainClass: String? = null
        parameters.sources.asFileTree.visit {
            if (!isDirectory && (name.endsWith(".java") || name.endsWith(".kt")) && file.readText().contains(regex)) {
                mainClass = relativePath.pathString.substringBeforeLast('.').replace('/', '.')
                stopVisiting()
            }
        }
        return mainClass
    }
}
