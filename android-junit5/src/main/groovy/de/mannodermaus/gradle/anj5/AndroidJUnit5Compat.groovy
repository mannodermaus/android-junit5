package de.mannodermaus.gradle.anj5

import com.android.build.gradle.internal.scope.VariantScope
import com.github.zafarkhaja.semver.Version

class AndroidJUnit5Compat {

    private static final Version AGP_2_2_0 = Version.valueOf("2.2.0-alpha1")
    private static final Version AGP_3_0_0 = Version.valueOf("3.0.0-alpha1")

    /**
     * Fetches the Java output directories for the given Variant scopes
     * across different versions of the Android Gradle plugin.
     * @param agpVersion Version of the Android Gradle Plugin
     * @param variantScope VariantScope to look up the Java outputs from
     * @see {@link VariantScope}
     * @return An Iterable container depicting the output directories
     */
    static Iterable<File> getJavaOutputDirs(Version agpVersion, def variantScope) {
        if (agpVersion.lessThan(AGP_2_2_0)) {
            // Below the first alpha of AGP 2.2.0, there was a typo in VariantScope
            // related to the Java outputs of a Variant
            return variantScope.javaOuptuts

        } else if (agpVersion.lessThan(AGP_3_0_0)) {
            // Below the first alpha of AGP 3.0.0, use the unified VariantScope method
            return variantScope.javaOutputs

        } else {
            // On and after AGP 3.0.0, use the separated methods for Java and annotation processors
            return [
                    variantScope.javaOutputDir as File,
                    variantScope.annotationProcessorOutputDir as File
            ]
        }
    }
}
