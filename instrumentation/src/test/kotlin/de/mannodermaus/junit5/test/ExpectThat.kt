package de.mannodermaus.junit5.test

import de.mannodermaus.junit5.test.ExpectedMessage.Containing
import de.mannodermaus.junit5.test.ExpectedMessage.EqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals

/*
 * Small utility framework for a fluent "Expectations API".
 * Allows for test declarations à la
 * "Expect that <method> throws <exception class> with message <string>".
 *
 * An Expectation starts with a invoking "expectThat()" and ends with "assert()".
 */

fun <T> expectThat(call: () -> T): ExpectThat<T> = ExpectThat(call)

class ExpectThat<E>
internal constructor(private val factory: () -> E) {

  fun <T : Throwable> willThrow(throwableClass: Class<T>) = ExpectThrowable(factory, throwableClass)
  fun willReturn(value: E) = ExpectValue(factory, value)
}

abstract class CompletedExpect<out E>
internal constructor(protected val factory: () -> E) {
  abstract fun assert()
}

class ExpectThrowable<E, T : Throwable>
internal constructor(
    factory: () -> E,
    private val throwableClass: Class<T>)
  : CompletedExpect<E>(factory) {

  private var messageConstraints: MutableList<ExpectedMessage> = mutableListOf()

  fun withMessage(vararg constraints: ExpectedMessage) = also {
    this.messageConstraints.addAll(constraints)
  }

  override fun assert() {
    val throwable = Assertions.assertThrows(throwableClass) {
      factory.invoke()
    }

    messageConstraints.forEach { expectedMessage ->
      val throwableMessage = throwable.message ?: ""
      expectedMessage.assertAgainst(throwableMessage)
    }
  }
}

class ExpectValue<out E>
internal constructor(
    factory: () -> E,
    private val value: E)
  : CompletedExpect<E>(factory) {

  override fun assert() {
    assertEquals(value, factory.invoke())
  }
}

/* Sealed Classes */

fun equalTo(expected: String) = EqualTo(expected)
fun containing(fragment: String) = Containing(fragment)

sealed class ExpectedMessage {
  abstract fun assertAgainst(actual: String)

  class EqualTo(private val expected: String) : ExpectedMessage() {
    override fun assertAgainst(actual: String) {
      assertEquals(expected, actual)
    }
  }

  class Containing(private val fragment: String) : ExpectedMessage() {
    override fun assertAgainst(actual: String) {
      assertThat(actual).containsOnlyOnce(fragment)
    }
  }
}
