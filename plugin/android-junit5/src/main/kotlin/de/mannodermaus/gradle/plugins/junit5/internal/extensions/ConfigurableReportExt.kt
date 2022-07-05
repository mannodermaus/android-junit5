package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.reporting.ConfigurableReport
import java.io.File

// Account for deprecated destination property
// and prefer outputLocation, if possible
@Suppress("DEPRECATION")
internal fun ConfigurableReport.setDestinationCompat(file: File) {
    when (val location = this.outputLocation) {
        is FileSystemLocationProperty<*> -> location.set(file)
        else -> this.destination = file
    }
}
