package de.mannodermaus.gradle.plugins.android_junit5

import com.android.build.gradle.internal.api.TestedVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.util.GradleVersion
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.PackagesExtension
import org.junit.platform.gradle.plugin.SelectorsExtension
import org.junit.platform.gradle.plugin.TagsExtension

/**
 * Android JUnit Platform plugin for Gradle.
 * Configures JUnit 5 tasks on all variants of an Android project.
 */
class AndroidJUnitPlatformPlugin implements Plugin<Project> {

    static final String LOG_TAG = "[android-junit5]"

    static final String EXTENSION_NAME = "junitPlatform"
    /**
     * This method doesn't call through to super.apply().
     * This is intentional, and prevents clashing between our Android-specific extension
     * & the junit-platform one.
     */
    @Override
    void apply(Project project) {
        // Validate that an Android plugin is applied
        if (!isAndroidProject(project)) {
            throw new ProjectConfigurationException("The android or android-library plugin must be applied to this project", null)
        }

        // Construct the platform extension (use our Android variant of the Extension class though)
        def junitExtension = project.extensions.create(EXTENSION_NAME, AndroidJUnitPlatformExtension, project)
        junitExtension.extensions.create("selectors", SelectorsExtension)
        junitExtension.extensions.create("filters", FiltersExtension)
        junitExtension.filters.extensions.create("packages", PackagesExtension)
        junitExtension.filters.extensions.create("tags", TagsExtension)
        junitExtension.filters.extensions.create("engines", EnginesExtension)

        // configuration.defaultDependencies used below was introduced in Gradle 2.5
        if (GradleVersion.current() < GradleVersion.version("2.5")) {
            throw new GradleException("android-junit5 plugin requires Gradle version 2.5 or higher")
        }

        // Add required JUnit Platform dependencies to a custom configuration that
        // will later be used to create the classpath for the custom task created
        // by this plugin.
        def configuration = project.configurations.maybeCreate("junitPlatform")
        configuration.defaultDependencies { deps ->
            // By default, include both TestEngines
            // and the Launcher-related dependencies
            // on the runtime classpath
            def platformVersion = junitExtension.platformVersion
            deps.add(project.dependencies.create("org.junit.platform:junit-platform-launcher:${platformVersion}"))
            deps.add(project.dependencies.create("org.junit.platform:junit-platform-console:${platformVersion}"))

            def jupiterVersion = junitExtension.jupiterVersion
            deps.add(project.dependencies.create("org.junit.jupiter:junit-jupiter-engine:${jupiterVersion}"))

            def vintageVersion = junitExtension.vintageVersion
            deps.add(project.dependencies.create("org.junit.vintage:junit-vintage-engine:${vintageVersion}"))
        }

        // Configure dependency handlers
        project.dependencies.ext.junit5 = {
            def jupiterVersion = junitExtension.jupiterVersion
            def platformVersion = junitExtension.platformVersion
            def vintageVersion = junitExtension.vintageVersion

            return [
                    project.dependencies.create("junit:junit:4.12"),
                    project.dependencies.create("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}"),
                    project.dependencies.create("org.junit.platform:junit-platform-engine:${platformVersion}"),

                    // Only needed to run tests in an Android Studio that bundles an older version
                    // (see also http://junit.org/junit5/docs/current/user-guide/#running-tests-ide-intellij-idea)
                    project.dependencies.create("org.junit.platform:junit-platform-launcher:$platformVersion"),
                    project.dependencies.create("org.junit.platform:junit-platform-console:$platformVersion"),
                    project.dependencies.create("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion"),
                    project.dependencies.create("org.junit.vintage:junit-vintage-engine:$vintageVersion")
            ]
        }

        project.dependencies.ext.junit5Params = {
            def jupiterVersion = junitExtension.jupiterVersion

            return project.dependencies.create("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
        }

        project.afterEvaluate {
            configure(project)
        }
    }

    private static void configure(Project project) {
        // Add the test task to each of the project's unit test variants
        def allVariants = isAndroidLibrary(project) ? "libraryVariants" : "applicationVariants"
        def testVariants = project.android[allVariants].findAll { it.hasProperty("unitTestVariant") }
        testVariants.each { variant -> AndroidJUnit5Test.create(project, variant as TestedVariant) }
    }

    private static boolean isAndroidProject(Project project) {
        return project.plugins.findPlugin("com.android.application") ||
                project.plugins.findPlugin("android") ||
                project.plugins.findPlugin("com.android.test") ||
                isAndroidLibrary(project)
    }

    private static boolean isAndroidLibrary(Project project) {
        return project.plugins.findPlugin("com.android.library") ||
                project.plugins.findPlugin("android-library")
    }
}
