import kotlin.String

/**
 * Generated by https://github.com/jmfayard/buildSrcVersions
 *
 * Update this file with
 *   `$ ./gradlew buildSrcVersions` */
object Libs {
    /**
     * https://developer.android.com/testing */
    const val espresso_core: String = "androidx.test.espresso:espresso-core:" +
            Versions.espresso_core

    /**
     * https://developer.android.com/testing */
    const val androidx_test_core: String = "androidx.test:core:" +
        Versions.androidx_test_core

    /**
     * https://developer.android.com/testing */
    const val androidx_test_monitor: String = "androidx.test:monitor:" +
        Versions.androidx_test_monitor

    /**
     * https://developer.android.com/testing */
    const val androidx_test_runner: String = "androidx.test:runner:" +
        Versions.androidx_test_runner

    /**
     * https://developer.android.com/studio */
    const val aapt2: String = "com.android.tools.build:aapt2:" + Versions.aapt2

    /**
     * https://developer.android.com/studio */
    const val com_android_tools_build_gradle: String = "com.android.tools.build:gradle:" +
            Versions.com_android_tools_build_gradle
    const val com_android_tools_build_gradle_32x: String = "com.android.tools.build:gradle:" +
        Versions.com_android_tools_build_gradle_32x
    const val com_android_tools_build_gradle_33x: String = "com.android.tools.build:gradle:" +
        Versions.com_android_tools_build_gradle_33x
    const val com_android_tools_build_gradle_34x: String = "com.android.tools.build:gradle:" +
    Versions.com_android_tools_build_gradle_34x
    const val com_android_tools_build_gradle_35x: String = "com.android.tools.build:gradle:" +
        Versions.com_android_tools_build_gradle_35x
    const val com_android_tools_build_gradle_36x: String = "com.android.tools.build:gradle:" +
        Versions.com_android_tools_build_gradle_36x

    /**
     * https://developer.android.com/studio */
    const val lint_gradle: String = "com.android.tools.lint:lint-gradle:" + Versions.lint_gradle

    /**
     * https://github.com/aNNiMON/Lightweight-Stream-API */
    const val stream: String = "com.annimon:stream:" + Versions.stream

    /**
     * https://github.com/ben-manes/gradle-versions-plugin */
    const val gradle_versions_plugin: String = "com.github.ben-manes:gradle-versions-plugin:" +
            Versions.gradle_versions_plugin

    /**
     * https://github.com/dcendents/android-maven-gradle-plugin */
    const val android_maven_gradle_plugin: String =
            "com.github.dcendents:android-maven-gradle-plugin:" +
            Versions.android_maven_gradle_plugin

    /**
     * https://github.com/Kotlin/dokka */
    const val dokka_core_plugin: String =
        "org.jetbrains.dokka:dokka-gradle-plugin:" +
            Versions.dokka

    /**
     * https://github.com/Kotlin/dokka */
    const val dokka_android_plugin: String =
        "org.jetbrains.dokka:dokka-android-gradle-plugin:" +
            Versions.dokka

    /**
     * https://github.com/zafarkhaja/jsemver */
    const val java_semver: String = "com.github.zafarkhaja:java-semver:" + Versions.java_semver

    const val gradle_bintray_plugin: String = "com.jfrog.bintray.gradle:gradle-bintray-plugin:" +
            Versions.gradle_bintray_plugin

    /**
     * https://github.com/google/truth */
    const val truth: String = "com.google.truth:truth:" +
            Versions.truth

    /**
     * https://dl.google.com/dl/android/maven2 */
    const val truth_android: String = "androidx.test.ext:truth:" +
            Versions.truth_android

    /**
     * http://commons.apache.org/proper/commons-io/ */
    const val commons_io: String = "commons-io:commons-io:" + Versions.commons_io

    /**
     * http://commons.apache.org/lang/ */
    const val commons_lang: String = "commons-lang:commons-lang:" + Versions.commons_lang

    const val de_fayard_buildsrcversions_gradle_plugin: String =
            "de.fayard.buildSrcVersions:de.fayard.buildSrcVersions.gradle.plugin:" +
            Versions.de_fayard_buildsrcversions_gradle_plugin

    const val android_junit5: String = "de.mannodermaus.gradle.plugins:android-junit5:" +
            Versions.android_junit5
    /**
     * https://mvnrepository.com/artifact/org.jacoco/org.jacoco.agent */
    const val org_jacoco_agent: String = "org.jacoco:org.jacoco.agent:" + Versions.org_jacoco_agent

    /**
     * https://mvnrepository.com/artifact/org.jacoco/org.jacoco.ant */
    const val org_jacoco_ant: String = "org.jacoco:org.jacoco.ant:" + Versions.org_jacoco_ant


    const val android_instrumentation_test_runner: String =
            "de.mannodermaus.junit5:android-instrumentation-test-runner:" +
            Versions.de_mannodermaus_junit5

    const val android_instrumentation_test: String =
            "de.mannodermaus.junit5:android-instrumentation-test:" + Versions.de_mannodermaus_junit5

    /**
     * https://github.com/wupdigital/android-maven-publish */
    const val android_maven_publish: String = "digital.wup:android-maven-publish:" +
            Versions.android_maven_publish

    /**
     * http://junit.org */
    const val junit: String = "junit:junit:" + Versions.junit

    /**
     * http://assertj.org */
    const val assertj_core: String = "org.assertj:assertj-core:" + Versions.assertj_core

    /**
     * https://kotlinlang.org/ */
    const val kotlin_compiler_embeddable: String =
            "org.jetbrains.kotlin:kotlin-compiler-embeddable:" + Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_gradle_plugin: String = "org.jetbrains.kotlin:kotlin-gradle-plugin:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_reflect: String = "org.jetbrains.kotlin:kotlin-reflect:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_scripting_compiler_embeddable: String =
            "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_stdlib_jdk8: String = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_stdlib: String = "org.jetbrains.kotlin:kotlin-stdlib:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://jetbrains.github.io/spek */
    const val spek_api: String = "org.jetbrains.spek:spek-api:" + Versions.org_jetbrains_spek

    /**
     * https://jetbrains.github.io/spek */
    const val spek_junit_platform_engine: String =
            "org.jetbrains.spek:spek-junit-platform-engine:" + Versions.org_jetbrains_spek

    /**
     * https://github.com/junit-pioneer/junit-pioneer */
    const val junit_pioneer: String = "org.junit-pioneer:junit-pioneer:" + Versions.junit_pioneer

    /**
     * http://junit.org/junit5/ */
    const val junit_jupiter_api: String = "org.junit.jupiter:junit-jupiter-api:" +
            Versions.org_junit_jupiter

    /**
     * http://junit.org/junit5/ */
    const val junit_jupiter_engine: String = "org.junit.jupiter:junit-jupiter-engine:" +
            Versions.org_junit_jupiter

    /**
     * http://junit.org/junit5/ */
    const val junit_jupiter_params: String = "org.junit.jupiter:junit-jupiter-params:" +
            Versions.org_junit_jupiter

    /**
     * http://junit.org/junit5/ */
    const val junit_platform_commons: String = "org.junit.platform:junit-platform-commons:" +
            Versions.org_junit_platform

    /**
     * http://junit.org/junit5/ */
    const val junit_platform_engine: String = "org.junit.platform:junit-platform-engine:" +
            Versions.org_junit_platform

    /**
     * http://junit.org/junit5/ */
    const val junit_platform_launcher: String = "org.junit.platform:junit-platform-launcher:" +
            Versions.org_junit_platform

    /**
     * http://junit.org/junit5/ */
    const val junit_platform_runner: String = "org.junit.platform:junit-platform-runner:" +
            Versions.org_junit_platform

    /**
     * http://junit.org/junit5/ */
    const val junit_vintage_engine: String = "org.junit.vintage:junit-vintage-engine:" +
            Versions.junit_vintage_engine

    /**
     * https://github.com/mockito/mockito */
    const val mockito_core: String = "org.mockito:mockito-core:" + Versions.mockito_core
}
