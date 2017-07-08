package de.mannodermaus.gradle.plugins.android_junit5

import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import com.android.builder.core.VariantType
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.junit.platform.console.ConsoleLauncher

/**
 * Task class used for unit tests driven by JUnit 5.
 * Its API mimics the Android Gradle Plugin's {@link AndroidUnitTest}
 * pretty closely, and it takes advantage of the efforts related to
 * classpath construction prevalent in the platform's default implementation.
 */
class AndroidJUnit5Test extends JavaExec {

    private static final String TASK_NAME_PREFIX = "junitPlatformTest"
    private static final String TASK_GROUP = JavaBasePlugin.VERIFICATION_GROUP
    private static final String TASK_DESCRIPTION = "Runs tests on the JUnit Platform."

    FileCollection resCollection
    FileCollection assetsCollection

    @InputFiles
    @Optional
    FileCollection getResCollection() {
        return resCollection
    }

    @InputFiles
    @Optional
    FileCollection getAssetsCollection() {
        return assetsCollection
    }

    static AndroidJUnit5Test create(Project project, TestedVariant variant) {
        def configAction = new ConfigAction(project, variant)
        return project.tasks.create(configAction.getName(), configAction.getType(), configAction)
    }

    /**
     * ConfigAction for a JUnit 5 task.
     */
    static class ConfigAction implements TaskConfigAction<AndroidJUnit5Test> {

        private final Project project
        private final TestedVariant variant
        private final VariantScope scope

        ConfigAction(Project project, TestedVariant variant) {
            this.project = project
            this.variant = variant
            this.scope = variant.variantData.scope
        }

        @Override
        String getName() {
            return scope.getTaskName(TASK_NAME_PREFIX)
        }

        @Override
        Class<AndroidJUnit5Test> getType() {
            return AndroidJUnit5Test.class
        }

        @Override
        void execute(AndroidJUnit5Test task) {
            task.setGroup(TASK_GROUP)
            task.setDescription(TASK_DESCRIPTION)

            // Configure JUnit 5 properties
            AndroidJUnitPlatformExtension junitExtension =
                    project.extensions.getByName(AndroidJUnitPlatformPlugin.EXTENSION_NAME) as AndroidJUnitPlatformExtension
            configureTaskInputs(task, junitExtension)
            configureTaskDependencies(task, junitExtension)
            def reportsDir = configureTaskOutputs(task, junitExtension)

            // Share the classpath with the default unit tests managed by Android,
            // but append the JUnit Platform configuration at the end
            //
            // Note: the user's test runtime classpath must come first; otherwise, code
            // instrumented by Clover in JUnit's build will be shadowed by JARs pulled in
            // via the junitPlatform configuration... leading to zero code coverage for
            // the respective modules.
            task.setClasspath(defaultUnitTestTask.classpath + project.configurations.junitPlatform)

            // Aggregate the source folders for test cases
            // (usually, the unit test variant's folders should be enough,
            // however we aggregate the main scope's output as well)
            def testRootDirs = [
                    // e.g. "build/intermediates/classes/debug/..."
                    scope.javaOutputDir,
                    // e.g. "build/intermediates/classes/test/debug/..."
                    variant.unitTestVariant.variantData.scope.javaOutputDir
            ]
            project.logger.info("$AndroidJUnitPlatformPlugin.LOG_TAG: Assembled JUnit 5 Task '$task.name':")
            testRootDirs.each {
                project.logger.info("$AndroidJUnitPlatformPlugin.LOG_TAG: |__ $it")
            }

            task.main = ConsoleLauncher.class.getName()
            task.args buildArgs(project, junitExtension, reportsDir, testRootDirs)

            project.logger.info("$AndroidJUnitPlatformPlugin.LOG_TAG: * JUnit 5 Arguments: $task.args")
        }

        /* Begin private */

        private void configureTaskInputs(AndroidJUnit5Test task, AndroidJUnitPlatformExtension junitExtension) {
            // Setup JUnit 5 properties
            task.inputs.property('enableStandardTestTask', junitExtension.enableStandardTestTask)
            task.inputs.property('selectors.uris', junitExtension.selectors.uris)
            task.inputs.property('selectors.files', junitExtension.selectors.files)
            task.inputs.property('selectors.directories', junitExtension.selectors.directories)
            task.inputs.property('selectors.packages', junitExtension.selectors.packages)
            task.inputs.property('selectors.classes', junitExtension.selectors.classes)
            task.inputs.property('selectors.methods', junitExtension.selectors.methods)
            task.inputs.property('selectors.resources', junitExtension.selectors.resources)
            task.inputs.property('filters.engines.include', junitExtension.filters.engines.include)
            task.inputs.property('filters.engines.exclude', junitExtension.filters.engines.exclude)
            task.inputs.property('filters.tags.include', junitExtension.filters.tags.include)
            task.inputs.property('filters.tags.exclude', junitExtension.filters.tags.exclude)
            task.inputs.property('filters.includeClassNamePatterns', junitExtension.filters.includeClassNamePatterns)
            task.inputs.property('filters.packages.include', junitExtension.filters.packages.include)
            task.inputs.property('filters.packages.exclude', junitExtension.filters.packages.exclude)

            if (junitExtension.logManager) {
                systemProperty 'java.util.logging.manager', junitExtension.logManager
            }
        }

        private def configureTaskOutputs(AndroidJUnit5Test task, AndroidJUnitPlatformExtension junitExtension) {
            def reportsDir = junitExtension.reportsDir ?: project.file("$project.buildDir/test-results/junit-platform")
            task.outputs.dir reportsDir

            return reportsDir
        }

        private def configureTaskDependencies(AndroidJUnit5Test task, AndroidJUnitPlatformExtension junitExtension) {
            // Connect to the default unit test task
            def variantUnitTestTask = this.defaultUnitTestTask
            if (variantUnitTestTask.hasProperty("resCollection")) {
                // 3.x provides additional input parameters
                task.resCollection = variantUnitTestTask.resCollection
                task.assetsCollection = variantUnitTestTask.assetsCollection
            }
            variantUnitTestTask.setEnabled(junitExtension.enableStandardTestTask)
            variantUnitTestTask.dependsOn task

            // Depend on the assembly of the test classes
            def defaultAssembleTestName = scope.getTaskName(
                    "assemble",
                    VariantType.UNIT_TEST.getSuffix())
            def variantAssembleTask = project.tasks.getByName(defaultAssembleTestName)
            task.dependsOn variantAssembleTask

            // Hook into the main test task
            def mainTestTask = project.tasks.getByName("test")
            mainTestTask.dependsOn task
        }

        private def getDefaultUnitTestTask() {
            def defaultUnitTestName = scope.getTaskName(
                    VariantType.UNIT_TEST.getPrefix(),
                    VariantType.UNIT_TEST.getSuffix())
            return project.tasks.getByName(defaultUnitTestName) as AndroidUnitTest
        }

        private List<String> buildArgs(project, junitExtension, reportsDir, testRootDirs) {
            def args = []

            args.add("--details ${junitExtension.details.toString()}")

            addSelectors(project, junitExtension.selectors, testRootDirs, args)
            addFilters(junitExtension.filters, args)

            args.add("--reports-dir")
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
    }
}
