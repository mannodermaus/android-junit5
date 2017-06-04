package de.mannodermaus.gradle.anj5

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.VariantScope
import com.github.zafarkhaja.semver.Version
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import org.gradle.api.Project

class AndroidJUnit5Compat {

    private static final int AGP_2 = 2
    private static final Version AGP_2_2_0 = Version.valueOf("2.2.0-alpha1")

    /**
     * Fetches the Java output directories for the given Variant scopes
     * across different versions of the Android Gradle plugin.
     * @param project Gradle project context
     * @param agpVersion Version of the Android Gradle Plugin
     * @param variantScope VariantScope to look up the Java outputs from
     * @see {@link VariantScope}
     * @return An Iterable container depicting the output directories
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    static Iterable<File> getJavaOutputDirs(Project project, Version agpVersion, def variantScope) {
        if (agpVersion.majorVersion <= AGP_2) {
            // AGP 2.x: Java Outputs & Annotation Processor outputs are aggregated.
            // Wee need to account for a typo that existed before 2.2.0
            def javaOutputs = agpVersion.lessThan(AGP_2_2_0) ?
                    variantScope.javaOuptuts :
                    variantScope.javaOutputs

            // Add the runtime configuration explicitly
            def testApk = project.configurations.findByName("testApk")

            return Iterables.concat(
                    javaOutputs,
                    testApk != null ?
                            ImmutableList.of(variantScope.annotationProcessorOutputDir, testApk) :
                            ImmutableList.of(variantScope.annotationProcessorOutputDir))

        } else {
            // AGP 3.x: Use the refined Java Classpath API to collect runtime elements.
            // Since we're querying the entire runtime classpath directly,
            // there's no need to add the runtime configuration ("testRuntimeOnly") explicitly
            return Iterables.concat(
                    variantScope.getJavaClasspath(
                            AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                            AndroidArtifacts.ArtifactType.CLASSES),
                    ImmutableList.of(variantScope.javaOutputDir))
        }
    }
}
