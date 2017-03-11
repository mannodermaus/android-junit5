package de.mannodermaus.gradle.anj5

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GradleVersion
import org.junit.platform.console.ConsoleLauncher
import org.junit.platform.gradle.plugin.*

import java.lang.reflect.Method

/**
 * Android JUnit Platform plugin for Gradle.
 *
 * Unfortunately, because of the restricted visibility of the plugin's members,
 * a lot of its functionality in regards to setup & configuration needs to be duplicated
 * in this class, even though it extends the "pure-Java plugin" and shares a lot
 * of its work.
 */
class AndroidJUnitPlatformPlugin extends JUnitPlatformPlugin {

    private static final String EXTENSION_NAME = 'junitPlatform'
    private static final String TASK_NAME = 'junitPlatformTest'

    /* Reflectively accessed because of restricted visibility in the super-class */
    private Method buildArgsMethod

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

        // Obtain a handle to the private "buildArgs" method, since that cuts down
        // on the amount of code duplication going on in this class
        try {
            buildArgsMethod = JUnitPlatformPlugin.class.getDeclaredMethod("buildArgs", Object.class, Object.class, Object.class)
            buildArgsMethod.setAccessible(true)
        } catch (NoSuchMethodError error) {
            throw new ProjectConfigurationException("Unexpected missing junit-plugin class definition.", error)
        }

        // Construct the platform extension (use our Android variant of the Extension class though)
        def junitExtension = project.extensions.create(EXTENSION_NAME, AndroidJUnit5PlatformExtension, project)
        junitExtension.extensions.create('selectors', SelectorsExtension)
        junitExtension.extensions.create('filters', FiltersExtension)
        junitExtension.filters.extensions.create('packages', PackagesExtension)
        junitExtension.filters.extensions.create('tags', TagsExtension)
        junitExtension.filters.extensions.create('engines', EnginesExtension)

        // configuration.defaultDependencies used below was introduced in Gradle 2.5
        if (GradleVersion.current().compareTo(GradleVersion.version('2.5')) < 0) {
            throw new GradleException('junit-platform-gradle-plugin requires Gradle version 2.5 or higher')
        }

        // Add required JUnit Platform dependencies to a custom configuration that
        // will later be used to create the classpath for the custom task created
        // by this plugin.
        def configuration = project.configurations.maybeCreate('junitPlatform')
        configuration.defaultDependencies { deps ->
            def version = junitExtension.platformVersion
            deps.add(project.dependencies.create("org.junit.platform:junit-platform-launcher:${version}"))
            deps.add(project.dependencies.create("org.junit.platform:junit-platform-console:${version}"))
        }

        // Add a junitJupiter() dependency handler
        project.dependencies.ext.junitJupiter = {
            def version = junitExtension.jupiterVersion
            project.dependencies.create("org.junit.jupiter:junit-jupiter-api:${version}")
            project.dependencies.create("org.junit.jupiter:junit-jupiter-engine:${version}")
        }

        project.afterEvaluate {
            configure(project, junitExtension)
        }
    }

    private void configure(Project project, AndroidJUnit5PlatformExtension junitExtension) {
        // Add the test task to each of the project's unit test variants
        def allVariants = isAndroidLibrary(project) ? "libraryVariants" : "applicationVariants"
        def testVariants = project.android[allVariants].findAll { it.hasProperty("unitTestVariant") }

        testVariants.collect { it.unitTestVariant }.each { variant ->
            def buildType = variant.buildType.name
            def nameSuffix = "${variant.flavorName.capitalize()}${buildType.capitalize()}"

            // Obtain variant properties
            def variantData = variant.variantData
            def variantScope = variantData.scope
            def scopeJavaOutputs = variantScope.hasProperty("javaOutputs") ? variantScope.javaOutputs : variantScope.javaOuptuts

            // Obtain tested variant properties
            def testedVariantData = variant.testedVariant.variantData
            def testedVariantScope = testedVariantData.scope
            def testedScopeJavaOutputs = testedVariantScope.hasProperty("javaOutputs") ? testedVariantScope.javaOutputs : testedVariantScope.javaOuptuts

            // Setup classpath for this variant's tests and add the test task
            // (refer to com.android.build.gradle.tasks.factory.UnitTestConfigAction)
            // 1) Add the compiler's classpath
            def classpaths = new ArrayList<>()
            if (variantData.javacTask) {
                def javaCompiler = variant.javaCompiler
                classpaths.add(javaCompiler.classpath)
                classpaths.add(javaCompiler.outputs.files)
            } else {
                classpaths.add(testedScopeJavaOutputs)
                classpaths.add(scopeJavaOutputs)
            }

            // 2) Add the testApk configuration
            def testApk = project.configurations.findByName("testApk")
            if (testApk != null) {
                classpaths.add(testApk)
            }

            // 3) Add test resources
            classpaths.add(variantData.javaResourcesForUnitTesting)
            classpaths.add(testedVariantData.javaResourcesForUnitTesting)

            // 4) Add filtered boot classpath
            def globalScope = variantScope.globalScope
            classpaths.add(globalScope.androidBuilder.getBootClasspath(false).findAll { it.name != "android.jar" })

            // 5) Add mocked version of android.jar
            classpaths.add(globalScope.mockableAndroidJarFile)

            addJunitPlatformTask(
                    project: project,
                    junitExtension: junitExtension,
                    nameSuffix: nameSuffix,
                    classpath: project.files(classpaths),
                    dependentTasks: Collections.singletonList("assemble${nameSuffix}UnitTest"))
        }
    }

    private void addJunitPlatformTask(Map<String, ?> map) {
        Project project = map.project as Project
        def junitExtension = map.junitExtension
        def classpath = map.classpath
        String nameSuffix = map.getOrDefault("nameSuffix", "")
        def dependentTasks = map.dependentTasks

        project.task(
                TASK_NAME + nameSuffix,
                type: JavaExec,
                group: 'verification',
                description: 'Runs tests on the JUnit Platform.') { junitTask ->

            junitTask.inputs.property('enableStandardTestTask', junitExtension.enableStandardTestTask)
            junitTask.inputs.property('selectors.uris', junitExtension.selectors.uris)
            junitTask.inputs.property('selectors.files', junitExtension.selectors.files)
            junitTask.inputs.property('selectors.directories', junitExtension.selectors.directories)
            junitTask.inputs.property('selectors.packages', junitExtension.selectors.packages)
            junitTask.inputs.property('selectors.classes', junitExtension.selectors.classes)
            junitTask.inputs.property('selectors.methods', junitExtension.selectors.methods)
            junitTask.inputs.property('selectors.resources', junitExtension.selectors.resources)
            junitTask.inputs.property('filters.engines.include', junitExtension.filters.engines.include)
            junitTask.inputs.property('filters.engines.exclude', junitExtension.filters.engines.exclude)
            junitTask.inputs.property('filters.tags.include', junitExtension.filters.tags.include)
            junitTask.inputs.property('filters.tags.exclude', junitExtension.filters.tags.exclude)
            junitTask.inputs.property('filters.includeClassNamePatterns', junitExtension.filters.includeClassNamePatterns)
            junitTask.inputs.property('filters.packages.include', junitExtension.filters.packages.include)
            junitTask.inputs.property('filters.packages.exclude', junitExtension.filters.packages.exclude)

            def reportsDir = junitExtension.reportsDir ?: project.file("$project.buildDir/test-results/junit-platform")
            junitTask.outputs.dir reportsDir

            if (junitExtension.logManager) {
                systemProperty 'java.util.logging.manager', junitExtension.logManager
            }

            configureTaskDependencies(project, junitTask, junitExtension, dependentTasks)

            // Build the classpath from the user's test runtime classpath and the JUnit
            // Platform modules.
            //
            // Note: the user's test runtime classpath must come first; otherwise, code
            // instrumented by Clover in JUnit's build will be shadowed by JARs pulled in
            // via the junitPlatform configuration... leading to zero code coverage for
            // the respective modules.
            junitTask.classpath = classpath + project.configurations.junitPlatform

            junitTask.main = ConsoleLauncher.class.getName()
            junitTask.args buildArgsMethod.invoke(this, project, junitExtension, reportsDir)
        }
    }

    private static void configureTaskDependencies(project, junitTask, junitExtension, dependentTasks) {
        if (!dependentTasks) {
            dependentTasks = Collections.emptyList()
        }

        dependentTasks.each {
            def task = project.tasks.findByName(it)
            if (task) {
                junitTask.dependsOn task
            }
        }

        def testTask = project.tasks.getByName('test')
        testTask.dependsOn junitTask
        testTask.enabled = junitExtension.enableStandardTestTask
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
