package de.mannodermaus.junit5.compose.internal

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch


// Bridge adapter API to ComposeRule, inspired by Webcompere's Java Test Gadgets:
// https://github.com/webcompere/java-test-gadgets/blob/e7b2b0628ee91862c59e85392bb2ab01345f8f61/test-gadgets-core/src/main/java/uk/org/webcompere/testgadgets/rules/DangerousRuleAdapter.java
internal class ComposeRuleAdapter(internal val rule: ComposeContentTestRule) {
    private var tearDown = CountDownLatch(1)
    private var tornDown = CountDownLatch(1)

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    fun setup() {
        val isActive = CountDownLatch(1)

        job = scope.launch {
            try {
                executeWithRule(rule) {
                    isActive.countDown()
                    tearDown.await()
                }
            } finally {
                tornDown.countDown()
            }
        }

        isActive.await()
    }

    fun teardown() {
        tearDown.countDown()
        tornDown.await()

        job?.cancel()
        tearDown = CountDownLatch(1)
        tornDown = CountDownLatch(1)
    }
}

private data class Box<T>(var value: T?)

private fun <T> executeWithRule(rule: TestRule, callable: () -> T): T? {
    val box = Box<T>(null)

    try {
        constructStatement(rule, callable, box).evaluate()
    } catch (t: Throwable) {
        if (t is Exception) {
            throw t
        } else if (t is Error) {
            throw t
        }
    }
    return box.value
}

private fun <T> constructStatement(rule: TestRule, callable: () -> T, box: Box<T>): Statement {
    return rule.apply(object : Statement() {
        override fun evaluate() {
            box.value = callable()
        }
    }, Description.createTestDescription("ComposeExtension", "executeWithRule"))
}
