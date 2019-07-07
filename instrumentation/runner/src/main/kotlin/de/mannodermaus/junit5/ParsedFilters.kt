package de.mannodermaus.junit5

import android.content.Context
import android.content.res.Resources
import org.junit.platform.engine.Filter
import org.junit.platform.launcher.TagFilter

private const val INSTRUMENTATION_FILTER_RES_FILE_NAME = "de_mannodermaus_junit5_filters"

/**
 * Holder class for the contents of a test plan.
 * It converts the contents of a resource file into JUnit Platform Filter objects
 * for the [AndroidJUnit5] runner.
 */
class ParsedFilters(private val filters: List<Filter<*>>) {

  companion object {
    val EMPTY = ParsedFilters(emptyList())

    @Suppress("FoldInitializerAndIfToElvis")
    fun fromContext(context: Context): ParsedFilters {
      // Look up the resource file written by the Gradle plugin
      // and open it.
      // (See Constants.kt inside the plugin's repository for the value used here)
      val identifier = context.resources.getIdentifier(INSTRUMENTATION_FILTER_RES_FILE_NAME, "raw", context.packageName)
      val inputStream = if (identifier != 0) {
        try {
          context.resources.openRawResource(identifier)
        } catch (rnfe: Resources.NotFoundException) {
          // Ignore
          null
        }
      } else {
        null
      }

      if (inputStream == null) {
        // File does't exist, or couldn't be located; return
        return EMPTY
      }

      // Try parsing the contents of the resource file
      // based on the expected format:
      // -t   Include Tag
      // -T   Exclude Tag
      val contents = inputStream.bufferedReader().readLines()
      val filters = mutableListOf<Filter<*>>()

      contents.forEach { line ->
        when {
          line.startsWith("-t ") -> filters += TagFilter.includeTags(line.substring(3))
          line.startsWith("-T ") -> filters += TagFilter.excludeTags(line.substring(3))
        }
      }

      return ParsedFilters(filters)
    }
  }

  fun all(): Array<Filter<*>> = filters.toTypedArray()
}
