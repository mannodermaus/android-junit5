package de.mannodermaus.junit5

import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.reflect.KClass

/**
 * A quick one-liner for executing a Jupiter discover-and-execute pass
 * from inside of a Jupiter test. Useful for testing runner code
 * that needs to work with the innards of the [TestPlan], such as
 * individual test identifiers and such.
 */
fun discoverTests(
    cls: KClass<*>,
    launcher: Launcher = LauncherFactory.create(),
    executeAsWell: Boolean = true,
): TestPlan {
    return launcher.discover(
        LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(cls.java))
            .build()
    ).also { plan ->
        if (executeAsWell) {
            launcher.execute(plan)
        }
    }
}
