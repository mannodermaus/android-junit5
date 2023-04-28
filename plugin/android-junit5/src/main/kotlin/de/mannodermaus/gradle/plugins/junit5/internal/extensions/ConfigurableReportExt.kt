package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.reporting.ConfigurableReport

internal val ConfigurableReport.outputLocationFile
    get() = outputLocation as? FileSystemLocationProperty<*>
