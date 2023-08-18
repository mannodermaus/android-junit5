package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.reporting.ConfigurableReport
import java.lang.reflect.Method

internal val ConfigurableReport.outputLocationFile: FileSystemLocationProperty<*>
    get() = try {
        outputLocation as FileSystemLocationProperty<*>
    } catch (e: NoSuchMethodError) {
        // Observed before Gradle 8.x
        getOutputLocationMethod.invoke(this) as FileSystemLocationProperty<*>
    }

private val ConfigurableReport.getOutputLocationMethod: Method
    get() = javaClass.declaredMethods.first { method ->
        method.name == "getOutputLocation" && method.returnType == FileSystemLocationProperty::class.java
    }
