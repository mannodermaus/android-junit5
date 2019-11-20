package de.mannodermaus.junit5.discovery

object PropertiesParser {

  @JvmStatic
  fun fromString(string: String) =
      string.split(",")
          .map { keyValuePair -> keyValuePair.split("=") }
          .filter { keyValueList -> keyValueList.size == 2 }
          .map { it[0] to it[1] }
          .toMap()
}
