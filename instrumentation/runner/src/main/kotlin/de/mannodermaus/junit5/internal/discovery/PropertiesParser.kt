package de.mannodermaus.junit5.internal.discovery

internal object PropertiesParser {
    fun fromString(string: String) =
        string.split(",")
            .map { keyValuePair -> keyValuePair.split("=") }
            .filter { keyValueList -> keyValueList.size == 2 }
            .associate { it[0] to it[1] }
}
