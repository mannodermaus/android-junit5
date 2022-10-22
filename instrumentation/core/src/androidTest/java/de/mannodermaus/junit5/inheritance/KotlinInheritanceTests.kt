package de.mannodermaus.junit5.inheritance

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

abstract class KotlinAbstractClass {
    @Test
    fun kotlinTest() {
        assertNotNull(getKotlinFileName())
    }

    abstract fun getKotlinFileName(): String?
}

interface KotlinInterface {
    @Test
    fun kotlinTest() {
        assert(kotlinValue > 0)
    }

    val kotlinValue: Int
}

class KotlinAbstractClassTest : KotlinAbstractClass() {
    override fun getKotlinFileName() = "hello world"
}

class KotlinInterfaceTest : KotlinInterface {
    override val kotlinValue: Int = 1337
}

class KotlinMixedInterfaceTest : KotlinInterface, JavaInterface {
    override val kotlinValue: Int = 1337
    override fun getJavaValue(): Long = 1234L
}
