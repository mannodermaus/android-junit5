package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantBuilder
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import com.android.builder.core.VariantType
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

internal inline fun <reified T : Task> TaskContainer.whenTaskAddedWithName(name: String, crossinline block: (T) -> Unit) =
    whenTaskAdded { task ->
        if (task is T && task.name == name) {
            block(task)
        }
    }

internal fun TaskContainer.testTaskOf(variant: Variant): TaskProvider<AndroidUnitTest>? {
    // From AGP 4.1 onwards, there is no Scope API on VariantData anymore.
    // Task names must be constructed manually
    val taskName = variant.getTaskName(
            prefix = VariantType.UNIT_TEST_PREFIX,
            suffix = VariantType.UNIT_TEST_SUFFIX
    )
    println("testTaskOf(name=${variant.name}, buildType=${variant.buildType}, flavorName=${variant.flavorName}, flavors=${variant.productFlavors}) ------> $taskName")

    return namedOrNull(taskName)
}
