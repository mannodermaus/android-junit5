package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import de.mannodermaus.gradle.plugins.junit5.VariantTypeCompat
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal inline fun <reified T : Task> TaskContainer.namedOrNull(name: String): TaskProvider<T>? =
        try {
            named(name, T::class.java)
        } catch (e: UnknownTaskException) {
            null
        }

internal fun TaskContainer.testTaskOf(variant: BaseVariant): TaskProvider<AndroidUnitTest>? {
    // From AGP 4.1 onwards, there is no Scope API on VariantData anymore.
    // Task names must be constructed manually
    val taskName = variant.getTaskName(
            prefix = VariantTypeCompat.UNIT_TEST_PREFIX,
            suffix = VariantTypeCompat.UNIT_TEST_SUFFIX)

    return namedOrNull(taskName)
}
