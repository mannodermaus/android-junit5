package de.mannodermaus.gradle.plugins.junit5.plugin

import de.mannodermaus.gradle.plugins.junit5.util.TestEnvironment
import de.mannodermaus.gradle.plugins.junit5.util.projects.PluginSpecProjectCreator
import org.gradle.api.Project
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A Junit 5 extension to provide the handle
 * to a Gradle project structure for testing.
 * Cleans up automatically after each test.
 */
class TestProjectProviderExtension : BeforeEachCallback, AfterEachCallback {

    private var factory: PluginSpecProjectCreator? = null
    private var rootProject: Project? = null

    override fun beforeEach(context: ExtensionContext) {
        val environment = TestEnvironment()
        val factory = PluginSpecProjectCreator(environment)
        val rootProject = factory.newRootProject()

        this.factory = factory
        this.rootProject = rootProject
    }

    override fun afterEach(context: ExtensionContext) {
        rootProject?.rootDir?.deleteRecursively()
    }

    fun newProject(): PluginSpecProjectCreator.Builder {
        val factory = this.factory!!
        val rootProject = this.rootProject!!

        return factory.newProject(rootProject)
    }
}
