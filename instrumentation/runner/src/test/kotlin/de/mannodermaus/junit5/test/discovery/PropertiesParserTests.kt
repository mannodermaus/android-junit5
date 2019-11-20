package de.mannodermaus.junit5.test.discovery

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.discovery.EnvironmentVariablesParser
import org.junit.jupiter.api.Test

class EnvironmentVariablesParserTests {

  @Test
  fun `test valid string, containing one entry`() {
    val string = "KEY1=true"
    val variables = EnvironmentVariablesParser.fromString(string)
    assertThat(variables).containsExactly(
        "KEY1", "true"
    )
  }

  @Test
  fun `test valid string, containing multiple entries`() {
    val string = "KEY1=true,KEY2=123,KEY3=lol"
    val variables = EnvironmentVariablesParser.fromString(string)
    assertThat(variables).containsExactly(
        "KEY1", "true",
        "KEY2", "123",
        "KEY3", "lol"
    )
  }

  @Test
  fun `test invalid string, filter out those entries`() {
    val string = "KEY1=true,INVALID1,INVALID2=lol=lolol,1234567"
    val variables = EnvironmentVariablesParser.fromString(string)
    assertThat(variables).containsExactly(
        "KEY1", "true"
    )
  }

  @Test
  fun `test invalid string, return empty map`() {
    val string = ""
    val variables = EnvironmentVariablesParser.fromString(string)
    assertThat(variables).isEmpty()
  }
}
