package de.mannodermaus.gradle.anj5

class AndroidJUnit5Compat {

    /**
     * Fetches the Java output directories for the given Variant scopes
     * across different versions of the Android Gradle plugin.
     * @param variantScope VariantScope to look up the Java outputs from
     * @return An Iterable container depicting the output directories
     */
    static Iterable<File> getJavaOutputDirs(variantScope) {
        if (variantScope.hasProperty("javaOutputs")) {
            return variantScope.javaOutputs

        } else if (variantScope.hasProperty("javaOuptuts")) {
            return variantScope.javaOuptuts

        } else {
            return Collections.singletonList(variantScope.javaOutputDir)
        }
    }
}
