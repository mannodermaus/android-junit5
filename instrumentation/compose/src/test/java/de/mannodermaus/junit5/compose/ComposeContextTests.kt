package de.mannodermaus.junit5.compose

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.Method

class ComposeContextTests {
    companion object {
        @JvmStatic
        fun relevantMethods() = buildList {
            addAll(ComposeTestRule::class.java.relevantMethods)
            addAll(ComposeContentTestRule::class.java.relevantMethods)
        }

        private val <T> Class<T>.relevantMethods
            get() = declaredMethods.filter { '$' !in it.name }
    }

    @MethodSource("relevantMethods")
    @ParameterizedTest(name = "ComposeContext defines {0} correctly")
    fun test(method: Method) {
        try {
            ComposeContext::class.java.getDeclaredMethod(method.name, *method.parameterTypes)
        } catch (ignored: NoSuchMethodException) {
            fail("ComposeContext does not define method $method")
        }
    }
}
