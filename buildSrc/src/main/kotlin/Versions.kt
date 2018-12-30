import kotlin.String

/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val espresso_core: String = "3.0.1" // available: "3.0.2"

    const val com_android_support_test_runner: String = "1.0.2" 

    const val aapt2: String = "3.2.1-4818971" 

    const val com_android_tools_build_gradle: String = "3.2.1" 

    const val lint_gradle: String = "26.2.1" 

    const val stream: String = "1.2.1" 

    const val gradle_versions_plugin: String = "0.20.0" 

    const val android_maven_gradle_plugin: String = "2.1" 

    const val java_semver: String = "0.9.0" 

    const val gradle_bintray_plugin: String = "1.8.4" 

    const val assertj_android: String = "1.2.0" 

    const val commons_io: String = "2.6" 

    const val commons_lang: String = "2.6" 

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2" 

    const val android_junit5: String = "1.3.2.0-SNAPSHOT" 

    const val de_mannodermaus_junit5: String = "0.2.2" 

    const val android_maven_publish: String = "3.6.2" 

    const val junit: String = "4.12" 

    const val assertj_core: String = "3.11.1" 

    const val org_jetbrains_kotlin: String = "1.3.11" 

    const val org_jetbrains_spek: String = "1.2.1" 

    const val junit_pioneer: String = "0.2.2" // available: "0.3.0"

    const val org_junit_jupiter: String = "5.3.2" 

    const val org_junit_platform: String = "1.3.2" 

    const val junit_vintage_engine: String = "5.3.2" 

    const val mockito_core: String = "2.19.0" // available: "2.23.4"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.0"

        const val currentVersion: String = "5.0"

        const val nightlyVersion: String = "5.2-20181230000028+0000"

        const val releaseCandidate: String = "5.1-rc-3"
    }
}
