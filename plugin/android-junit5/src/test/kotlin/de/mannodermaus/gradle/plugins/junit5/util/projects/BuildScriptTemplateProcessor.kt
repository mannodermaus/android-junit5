package de.mannodermaus.gradle.plugins.junit5.util.projects

import kotlin.math.max

// Known tokens
private val GET_MATCHER = Regex("//\\\$GET\\{(.*)}")
private val IF_MATCHER = Regex("//\\\$IF\\{(.*)}")
private val ELSE_MATCHER = Regex("//\\\$ELSE")
private val IFGRADLE_MATCHER = Regex("//\\\$IFGRADLE\\{(.*)}")
private val END_MATCHER = Regex("//\\\$END")

/**
 * Processor class for virtual build script files, used by Functional Tests.
 * It utilizes a very, very crude token API for placeholders, which are dynamically
 * injected into the virtual projects, based around template files located within src/test/resources.
 */
class BuildScriptTemplateProcessor(private val targetGradleVersion: String?,
                                   private val replacements: Map<String, Any>) {

  fun process(rawText: String): String {
    var ignoredBlockCount = 0

    // Replace GET tokens first
    val text1 = rawText.replace(GET_MATCHER) { result ->
      val key = result.groupValues.last()
      if (key in replacements) {
        "\"${replacements[key].toString()}\""
      } else {
        "\"(missing: '$key')\""
      }
    }

    // Iterate over the result and exclude all non-matching IF token blocks
    val text2 = StringBuilder()
    for (line in text1.lines()) {
      // Check if any conditional marker is included in the line
      val ifMatch = IF_MATCHER.find(line)
      val elseMatch = ELSE_MATCHER.find(line)
      val ifgradleMatch = IFGRADLE_MATCHER.find(line)
      val endMatch = END_MATCHER.find(line)

      if (ignoredBlockCount == 0 && ifMatch != null) {
        val ifKey = ifMatch.groupValues.last()
        val conditionEnabled = replacements[ifKey]
            ?.toString()
            ?.toBoolean()
            ?: false

        if (!conditionEnabled) {
          // Ignore this block
          ignoredBlockCount++
        }
      }

      if (ignoredBlockCount == 0 && ifgradleMatch != null) {
        val ifgradleExpression = ifgradleMatch.groupValues.last()

        // When the given Gradle requirement is null, or if the requirement
        // is not matched by the block, ignore it
        if (targetGradleVersion == null || !targetGradleVersion.startsWith(ifgradleExpression)) {
          // Ignore this block
          ignoredBlockCount++
        }
      }

      if (elseMatch != null) {
        ignoredBlockCount = if (ignoredBlockCount == 0) {
          1
        } else {
          max(0, ignoredBlockCount - 1)
        }
      }

      // Lines with a marker shouldn't be appended in the first place;
      // normal lines are appended only if we are not excluding a previously false conditional
      val isNormalLine = ifMatch == null && ifgradleMatch == null && elseMatch == null && endMatch == null
      if (isNormalLine && ignoredBlockCount == 0) {
        text2.append(line).appendln()
      }

      if (endMatch != null) {
        ignoredBlockCount = max(0, ignoredBlockCount - 1)
      }
    }
    return text2.toString()
  }
}
