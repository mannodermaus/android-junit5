package de.mannodermaus.gradle.plugins.junit5

import com.google.common.truth.Truth.assertThat
import de.mannodermaus.gradle.plugins.junit5.internal.utils.IncludeExcludeContainer
import org.junit.jupiter.api.Test

/** Created by Marcel Schnelle on 2018/06/28. */
class IncludeExcludeContainerTests {

    @Test
    fun `adding an include rule will remove an existing exclude rule`() {
        val container =
            IncludeExcludeContainer().apply {
                exclude("slow")
                include("slow")
            }

        assertThat(container.include).containsExactly("slow")
        assertThat(container.exclude).isEmpty()
    }

    @Test
    fun `adding an exclude rule will remove an existing include rule`() {
        val container =
            IncludeExcludeContainer().apply {
                include("slow")
                exclude("slow")
            }

        assertThat(container.include).isEmpty()
        assertThat(container.exclude).containsExactly("slow")
    }

    @Test
    fun `adding the same include rule twice will only add a single entry`() {
        val container =
            IncludeExcludeContainer().apply {
                include("slow")
                include("slow")
            }

        assertThat(container.include).containsExactly("slow")
    }

    @Test
    fun `adding the same exclude rule twice will only add a single entry`() {
        val container =
            IncludeExcludeContainer().apply {
                exclude("slow")
                exclude("slow")
            }

        assertThat(container.exclude).containsExactly("slow")
    }

    @Test
    fun `emptiness is properly reported for include rules`() {
        val container = IncludeExcludeContainer()
        assertThat(container.isEmpty()).isTrue()

        container.include("slow")
        assertThat(container.isEmpty()).isFalse()
    }

    @Test
    fun `emptiness is properly reported for exclude rules`() {
        val container = IncludeExcludeContainer()
        assertThat(container.isEmpty()).isTrue()

        container.exclude("slow")
        assertThat(container.isEmpty()).isFalse()
    }

    @Test
    fun `adding an empty container returns the original one`() {
        val container1 = IncludeExcludeContainer().apply { include("slow") }
        val container2 = IncludeExcludeContainer()
        val merged = container1 + container2
        assertThat(merged).isEqualTo(container1)
    }

    @Test
    fun `adding something to an empty container returns the new one`() {
        val container1 = IncludeExcludeContainer()
        val container2 = IncludeExcludeContainer().apply { include("slow") }
        val merged = container1 + container2
        assertThat(merged).isEqualTo(container2)
    }

    @Test
    fun `adding two conainers will merge the include rules together`() {
        val container1 = IncludeExcludeContainer().apply { include("slow") }
        val container2 = IncludeExcludeContainer().apply { include("fast") }
        val merged = container1 + container2

        assertThat(merged.include).containsExactly("slow", "fast")
        assertThat(merged.exclude).isEmpty()
    }

    @Test
    fun `adding two containers will merge the exclude rules together`() {
        val container1 = IncludeExcludeContainer().apply { exclude("slow") }
        val container2 = IncludeExcludeContainer().apply { exclude("fast") }
        val merged = container1 + container2

        assertThat(merged.include).isEmpty()
        assertThat(merged.exclude).containsExactly("slow", "fast")
    }

    @Test
    fun `adding two containers will remove an existing include rule with a second object's exclude rule`() {
        val container1 = IncludeExcludeContainer().apply { include("slow") }
        val container2 = IncludeExcludeContainer().apply { exclude("slow") }
        val merged = container1 + container2

        assertThat(merged.include).isEmpty()
        assertThat(merged.exclude).containsExactly("slow")
    }

    @Test
    fun `adding two containers will remove an existing exclude rule with a second object's include rule`() {
        val container1 = IncludeExcludeContainer().apply { exclude("slow") }
        val container2 = IncludeExcludeContainer().apply { include("slow") }
        val merged = container1 + container2

        assertThat(merged.include).containsExactly("slow")
        assertThat(merged.exclude).isEmpty()
    }

    @Test
    fun `adding two containers won't touch unrelated rules`() {
        val container1 =
            IncludeExcludeContainer().apply {
                include("fast")
                include("slow")
            }
        val container2 =
            IncludeExcludeContainer().apply {
                exclude("another")
                exclude("slow")
            }
        val merged = container1 + container2

        assertThat(merged.include).containsExactly("fast")
        assertThat(merged.exclude).containsAtLeast("slow", "another")
    }
}
