package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.file.FileSystemLocationProperty
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.reporting.SingleFileReport

internal val DirectoryReport.outputLocationFile
    get() = outputLocation as? FileSystemLocationProperty<*>

internal val SingleFileReport.outputLocationFile
    get() = outputLocation as? FileSystemLocationProperty<*>
