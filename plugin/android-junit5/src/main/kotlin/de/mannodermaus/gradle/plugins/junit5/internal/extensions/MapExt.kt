package de.mannodermaus.gradle.plugins.junit5.internal.extensions

internal fun Map<String, String>.getAsList(key: String, delimiter: String = ","): List<String> =
        this[key]?.split(delimiter) ?: emptyList()
