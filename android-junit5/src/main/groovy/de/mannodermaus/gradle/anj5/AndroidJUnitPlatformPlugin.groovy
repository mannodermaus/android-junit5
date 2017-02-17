package de.mannodermaus.gradle.anj5

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GradleVersion
import org.junit.platform.console.ConsoleLauncher
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.PackagesExtension
import org.junit.platform.gradle.plugin.SelectorsExtension
import org.junit.platform.gradle.plugin.TagsExtension

public class AndroidJUnitPlatformPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = 'junitPlatform';
    private static final String TASK_NAME      = 'junitPlatformTest';

    void apply(Project project) {
        // Validate that an Android plugin is applied
        if (!isAndroidProject(project)) {
            throw new ProjectConfigurationException("The android or android-library plugin must be applied to this project", null)
        }

        // Validate that the JUnit 5 Gradle plugin is available on the class path
        try {
            Class.forName("org.junit.platform.gradle.plugin.JUnitPlatformExtension")
        } catch (t) {
            throw new ProjectConfigurationException("The junit-platform-gradle-plugin needs to be added as a classpath dependency", t)
        }

        def junitExtension = project.extensions.create(EXTENSION_NAME, AndroidJUnit5Extension, project)
        junitExtension.extensions.create('selectors', SelectorsExtension)
        junitExtension.extensions.create('filters', FiltersExtension)
        junitExtension.filters.extensions.create('packages', PackagesExtension)
        junitExtension.filters.extensions.create('tags', TagsExtension)
        junitExtension.filters.extensions.create('engines', EnginesExtension)

        // configuration.defaultDependencies used below was introduced in Gradle 2.5
        if (GradleVersion.current().compareTo(GradleVersion.version('2.5')) < 0) {
            throw new GradleException('android-junit5 plugin requires Gradle version 2.5 or higher')
        }

        // Add required JUnit Platform dependencies to a custom configuration that
        // will later be used to create the classpath for the custom task created
        // by this plugin.
        def junitConf = project.configurations.maybeCreate('junitPlatform')
        junitConf.defaultDependencies { deps ->
            def version = junitExtension.platformVersion
            if (version == null) {
                version = readVersionFromPropertiesFile()
            }
            deps.add(project.dependencies.create("org.junit.platform:junit-platform-launcher:${version}"))
            deps.add(project.dependencies.create("org.junit.platform:junit-platform-console:${version}"))
        }

        // Add a junit5() dependency handler
        project.dependencies.ext.junitJupiter = {
            def version = junitExtension.jupiterVersion
            project.dependencies.create("org.junit.jupiter:junit-jupiter-api:${version}")
            project.dependencies.create("org.junit.jupiter:junit-jupiter-engine:${version}")
        }

        project.afterEvaluate {
            configure(project, junitExtension)
        }
    }

    private String readVersionFromPropertiesFile() {
        Properties properties = new Properties()
        getClass().getResourceAsStream("version.properties").withCloseable { inputStream ->
            properties.load(inputStream)
        }
        return properties.getProperty("version")
    }

    private void configure(Project project, junitExtension) {
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
            classpaths.add(project.configurations.getByName("testApk"))

            // 3) Add test resources
            classpaths.add(variantData.javaResourcesForUnitTesting)
            classpaths.add(testedVariantData.javaResourcesForUnitTesting)

            // 4) Add filtered boot classpath
            def globalScope = variantScope.globalScope
            classpaths.add(globalScope.androidBuilder.getBootClasspath(false).findAll {
                !it.name.equals("android.jar")
            })

            // 5) Add mocked version of android.jar
            classpaths.add(globalScope.mockableAndroidJarFile)

            addJunitPlattformTask(
                    project: project,
                    junitExtension: junitExtension,
                    nameSuffix: nameSuffix,
                    classpath: project.files(classpaths),
                    dependentTasks: Collections.singletonList("assemble${nameSuffix}UnitTest"))
        }
    }

    private void addJunitPlattformTask(Map map) {
        Project project = map.project
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

            def reportsDir = junitExtension.reportsDir ?: project.file("build/test-results/junit-platform")
            junitTask.outputs.dir reportsDir
            junitTask.outputs.upToDateWhen { false }

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
            junitTask.args buildArgs(project, junitExtension, reportsDir)
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

    private List<String> buildArgs(project, junitExtension, reportsDir) {

        def args = []

        if (junitExtension.details) {
            args.add('--details')
            args.add(junitExtension.details.name())
        }

        addSelectors(project, junitExtension.selectors, args)
        addFilters(junitExtension.filters, args)

        args.add('--reports-dir')
        args.add(reportsDir.getAbsolutePath())

        return args
    }

    private void addFilters(filters, args) {
        filters.includeClassNamePatterns.each { pattern ->
            args.addAll(['-n', pattern])
        }
        filters.excludeClassNamePatterns.each { pattern ->
            args.addAll(['-N', pattern])
        }
        filters.packages.include.each { includedPackage ->
            args.addAll(['--include-package',includedPackage])
        }
        filters.packages.exclude.each { excludedPackage ->
            args.addAll(['--exclude-package',excludedPackage])
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

    private void addSelectors(project, selectors, args) {
        if (selectors.empty) {
            def rootDirs = []
            project.sourceSets.each { sourceSet ->
                rootDirs.add(sourceSet.output.classesDir)
                rootDirs.add(sourceSet.output.resourcesDir)
                rootDirs.addAll(sourceSet.output.dirs.files)
            }
            args.addAll(['--scan-class-path', rootDirs.join(File.pathSeparator)])
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