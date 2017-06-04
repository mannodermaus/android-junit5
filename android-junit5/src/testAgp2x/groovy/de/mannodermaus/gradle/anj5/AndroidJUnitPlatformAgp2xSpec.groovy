package de.mannodermaus.gradle.anj5

import org.gradle.api.Project
import org.gradle.internal.resolve.ModuleVersionNotFoundException
import org.gradle.testfixtures.ProjectBuilder

/**
 * Unit Tests of the android-junit5 plugin for the Android Gradle Plugin 2.x.
 * This is applied on top of the default test cases that this class inherits.
 */
class AndroidJUnitPlatformAgp2xSpec extends AndroidJUnitPlatformSpec {

    @Override
    protected String testCompileDependency() {
        return "testCompile"
    }

    @Override
    protected String testRuntimeDependency() {
        return "testApk"
    }
}
