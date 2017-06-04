package de.mannodermaus.gradle.anj5

import com.android.build.gradle.internal.scope.VariantScope
import com.github.zafarkhaja.semver.Version
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GradleVersion
import org.junit.platform.console.ConsoleLauncher
import org.junit.platform.gradle.plugin.*

/**
 * Android JUnit Platform plugin for Gradle.
 *
 * Unfortunately, because of the restricted visibility of the plugin's members,
 * a lot of its functionality in regards to setup & configuration needs to be duplicated
 * in this class, even though it extends the "pure-Java plugin" and shares some
 * of its work.
 */
class AndroidJUnitPlatformPlugin extends JUnitPlatformPlugin {

    private static final String LOG_TAG = "[android-junit5]"

    private static final String VINTAGE_WARNING = "AGPBI: {\"kind\":\"warning\",\"text\":\"$LOG_TAG You don't need to depend on junitVintage() directly anymore!\",\"sources\":[{},{}]}"

    private static final String EXTENSION_NAME = 'junitPlatform'
    private static final String TASK_NAME = 'junitPlatformTest'

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

        // Add a junitJupiter() dependency handler
        project.dependencies.ext.junitJupiter = {
            def jupiterVersion = junitExtension.jupiterVersion

            return [
                    project.dependencies.create("junit:junit:4.12"),
                    project.dependencies.create("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}"),
            ]
        }

        // Add a junitParams() dependency handler
        project.dependencies.ext.junitParams = {
            def jupiterVersion = junitExtension.jupiterVersion

            return project.dependencies.create("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
        }

        // Add a junitVintage() dependency handler
        project.dependencies.ext.junitVintage = {
            project.logger.warn(VINTAGE_WARNING)
            return []
        }

        project.afterEvaluate {
            configure(project, junitExtension)
        }
    }

    private void configure(Project project, AndroidJUnit5PlatformExtension junitExtension) {
        def agpVersion = Version.valueOf(com.android.builder.Version.ANDROID_GRADLE_PLUGIN_VERSION)

        // Add the test task to each of the project's unit test variants
        def allVariants = isAndroidLibrary(project) ? "libraryVariants" : "applicationVariants"
        def testVariants = project.android[allVariants].findAll { it.hasProperty("unitTestVariant") }

        testVariants.collect { it.unitTestVariant }.each { variant ->
            def buildType = variant.buildType.name
            def nameSuffix = "${variant.flavorName.capitalize()}${buildType.capitalize()}"

            // Obtain variant properties
            def variantData = variant.variantData
            VariantScope variantScope = variantData.scope
            def scopeJavaOutputs = AndroidJUnit5Compat.getJavaOutputDirs(agpVersion, variantScope)

            // Obtain tested variant properties
            def testedVariantData = variant.testedVariant.variantData
            VariantScope testedVariantScope = testedVariantData.scope

            def testedScopeJavaOutputs = AndroidJUnit5Compat.getJavaOutputDirs(agpVersion, testedVariantScope)

            // Collect the root directories for unit tests from the variant's scopes
            def testRootDirs = []
            testRootDirs.add(variantScope.javaOutputDir)
            testRootDirs.add(testedVariantScope.javaOutputDir)

            // Setup classpath for this variant's tests and add the test task
            // (refer to com.android.build.gradle.tasks.factory.UnitTestConfigAction)
            // 1) Add the compiler's classpath
            def classpath = []
            if (variantData.javacTask) {
                def javaCompiler = variant.javaCompiler
                classpath.add(javaCompiler.classpath)
                classpath.add(javaCompiler.outputs.files)
            } else {
                classpath.add(testedScopeJavaOutputs)
                classpath.add(scopeJavaOutputs)
            }

            // 2) Add the runtime configurations
            def testRuntime = project.configurations.findByName("testRuntimeOnly")
            if (testRuntime == null) {
                testRuntime = project.configurations.findByName("testApk")
            }
            if (testRuntime != null) {
                classpath.add(testRuntime)
            }

            // 3) Add test resources
            classpath.add(variantData.javaResourcesForUnitTesting)
            classpath.add(testedVariantData.javaResourcesForUnitTesting)

            // 4) Add filtered boot classpath
            def globalScope = variantScope.globalScope
            classpath.add(globalScope.androidBuilder.getBootClasspath(false).findAll {
                it.name != "android.jar"
            })

            // 5) Add mocked version of android.jar
            classpath.add(globalScope.mockableAndroidJarFile)

            addJunitPlatformTask(
                    project: project,
                    junitExtension: junitExtension,
                    nameSuffix: nameSuffix,
                    classpath: project.files(classpath),
                    testRootDirs: testRootDirs,
                    dependentTasks: Collections.singletonList("assemble${nameSuffix}UnitTest"))
        }
    }

    private void addJunitPlatformTask(Map<String, ?> map) {
        Project project = map.project as Project
        def junitExtension = map.junitExtension
        def classpath = map.classpath
        def testRootDirs = map.testRootDirs
        String nameSuffix = map.getOrDefault("nameSuffix", "")
        def dependentTasks = map.dependentTasks

        project.task(
                TASK_NAME + nameSuffix,
                type: JavaExec,
                group: 'verification',
                description: 'Runs tests on the JUnit Platform.') { junitTask ->

            // Disable the default Unit Test task, since we're running JUnit 5 anyway
            def defaultTestTask = project.tasks.findByName("test${nameSuffix}UnitTest")
            defaultTestTask.setEnabled(false)
            defaultTestTask.dependsOn += junitTask

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
            junitTask.args buildArgs(project, junitExtension, reportsDir, testRootDirs)
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

    private List<String> buildArgs(project, junitExtension, reportsDir, testRootDirs) {

        def args = ['--hide-details']

        addSelectors(project, junitExtension.selectors, testRootDirs, args)
        addFilters(junitExtension.filters, args)

        args.add('--reports-dir')
        args.add(reportsDir.getAbsolutePath())

        return args
    }

    private void addFilters(filters, args) {
        filters.includeClassNamePatterns.each { pattern ->
            args.addAll(['-n', pattern])
        }
        filters.packages.include.each { includedPackage ->
            args.addAll(['--include-package', includedPackage])
        }
        filters.packages.exclude.each { excludedPackage ->
            args.addAll(['--exclude-package', excludedPackage])
        }
        filters.tags.include.each { tag ->
            args.addAll(['-t', tag])
        }
        filters.tags.exclude.each { tag ->
            args.addAll(['-T', tag])
        }
        filters.engines.include.each { engineId ->
            args.addAll(['-e', engineId])
        }
        filters.engines.exclude.each { engineId ->
            args.addAll(['-E', engineId])
        }
    }

    private void addSelectors(project, selectors, testRootDirs, args) {
        if (selectors.empty) {
            args.addAll(['--scan-class-path', testRootDirs.join(File.pathSeparator)])
        } else {
            selectors.uris.each { uri ->
                args.addAll(['-u', uri])
            }
            selectors.files.each { file ->
                args.addAll(['-f', file])
            }
            selectors.directories.each { directory ->
                args.addAll(['-d', directory])
            }
            selectors.packages.each { aPackage ->
                args.addAll(['-p', aPackage])
            }
            selectors.classes.each { aClass ->
                args.addAll(['-c', aClass])
            }
            selectors.methods.each { method ->
                args.addAll(['-m', method])
            }
            selectors.resources.each { resource ->
                args.addAll(['-r', resource])
            }
        }
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
