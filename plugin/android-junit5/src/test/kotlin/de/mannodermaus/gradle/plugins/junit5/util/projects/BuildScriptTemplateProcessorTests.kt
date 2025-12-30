package de.mannodermaus.gradle.plugins.junit5.util.projects

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BuildScriptTemplateProcessorTests {

  private lateinit var folder: File

  @BeforeEach
  fun before(@TempDir folder: File) {
    this.folder = folder
  }

  @Test
  fun `works with brackets`() = runTest(
    replacements = mapOf(
      "VALUE" to "hello"
    ),
    rawText = """
      val value = "{{ VALUE }}"
      if (123 == 456) {
        println("yolo")
      }
    """,
    expectedText = """
      val value = "hello"
      if (123 == 456) {
        println("yolo")
      }
    """
  )

  @Test
  fun `substitution with existing value`() = runTest(
    replacements = mapOf(
      "VALUE" to "hello"
    ),
    rawText = """
      val value = "{{ VALUE }}"
    """,
    expectedText = """
      val value = "hello"
    """
  )

  @Test
  fun `substitution with missing value`() = runTest(
    replacements = mapOf(),
    rawText = """
      val value = "{{ VALUE }}"
    """,
    expectedText = """
      val value = ""
    """
  )

  @Test
  fun `ifelse with first block winning`() = runTest(
    replacements = mapOf(
      "VALUE" to true
    ),
    rawText = """
      {% if VALUE %}
      val value = 1234
      {% else %}
      val value = 5678
      {% endif %}
    """,
    expectedText = """
      val value = 1234
    """
  )

  @Test
  fun `ifelse with second block winning`() = runTest(
    replacements = mapOf(
      "VALUE" to false
    ),
    rawText = """
      {% if VALUE %}
      val value = 1234
      {% else %}
      val value = 5678
      {% endif %}
    """,
    expectedText = """
      val value = 5678
    """
  )

  @Test
  fun `ifelseif with first block winning`() = runTest(
    replacements = mapOf(
      "VALUE" to true,
      "VALUE2" to true
    ),
    rawText = """
      {% if VALUE %}
      val value = 1234
      {% elseif VALUE2 %}
      val value = 5678
      {% endif %}
    """,
    expectedText = """
      val value = 1234
    """
  )

  @Test
  fun `ifelseif with second block winning`() = runTest(
    replacements = mapOf(
      "VALUE" to false,
      "VALUE2" to true
    ),
    rawText = """
      {% if VALUE %}
      val value = 1234
      {% elseif VALUE2 %}
      val value = 5678
      {% endif %}
    """,
    expectedText = """
      val value = 5678
    """
  )

  @Test
  fun `ifelseif with third block winning`() = runTest(
    replacements = mapOf(
      "VALUE" to false,
      "VALUE2" to false
    ),
    rawText = """
      {% if VALUE %}
      val value = 1234
      {% elseif VALUE2 %}
      val value = 5678
      {% else %}
      val value = 9999
      {% endif %}
    """,
    expectedText = """
      val value = 9999
    """
  )

  @Test
  fun `empty loop won't emit anything`() = runTest(
    replacements = mapOf(
      "VALUES" to listOf<String>()
    ),
    rawText = """
      println("before")
      {% for item in VALUES %}
        println(item)
      {% end %}
      println("after")
    """,
    expectedText = """
      println("before")
      
      println("after")
    """
  )

  @Test
  fun `loop will emit something`() = runTest(
    replacements = mapOf(
      "VALUES" to listOf("value 1", "value 2")
    ),
    rawText = """
      println("before")
      {% for item in VALUES %}
        println("{{ item }}")
      {% end %}
      println("after")
    """,
    expectedText = """
      println("before")
      
        println("value 1")
      
        println("value 2")
      
      println("after")
    """
  )

  @Test
  fun `loop with nested conditional`() = runTest(
    replacements = mapOf(
      "ENABLED" to false,
      "VALUES" to listOf("value 1", "value 2")
    ),
    rawText = """
      println("before")
      {% for item in VALUES %}
        {% if ENABLED %}println("{{ item }}"){% else %}print("{{ item }}"){% endif %}
      {% end %}
      println("after")
    """,
    expectedText = """
      println("before")
      
        print("value 1")
      
        print("value 2")
      
      println("after")
    """
  )

  @Test
  fun `custom functions work`() = runTest(
    replacements = emptyMap(),
    rawText = """
      {% if atLeastAgp("4.2") %}println("AGP 1"){% endif %}
      {% if atLeastAgp("1000.0") %}println("AGP 2"){% endif %}
      {% if atLeastGradle("5.0") %}println("GRD 1"){% endif %}
      {% if atLeastGradle("1000.0") %}println("GRD 2"){% endif %}
      {% if atLeastJUnit("5.10.0") %}println("JU 1"){% endif %}
      {% if atLeastJUnit("1000.4.0") %}println("JU 2"){% endif %}
    """,
    expectedText = """
      println("AGP 1")
      
      println("GRD 1")
      
      println("JU 1")
      
    """
  )

  /* Private */

  private fun runTest(replacements: Map<String, Any>, rawText: String, expectedText: String) = runBlocking {
    // Dump the content into a temporary file with a known name,
    // then run the processor over it and check its result
    val file = File(folder, "file.kt").also { it.writeText(rawText) }
    val processor = BuildScriptTemplateProcessor(
      folder = folder,
      replacements = replacements,
      agpVersion = "7.0",
      gradleVersion = "6.7",
      junitVersion = "5.14.1",
    )

    // Replace pound-signs with dollar signs (the tests use the former
    // to avoid annoyances with Kotlin's string interpolation syntax)
    assertThat(processor.process(file.name).trimIndent().trim())
      .isEqualTo(expectedText.trimIndent().trim())
  }
}