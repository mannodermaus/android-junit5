package de.mannodermaus.gradle.plugins.android_junit5

class AndroidJUnitPlatformAgp3xSpec extends AndroidJUnitPlatformSpec {

    @Override
    protected String testCompileDependency() {
        return "testApi"
    }

    @Override
    protected String testRuntimeDependency() {
        return "testRuntimeOnly"
    }
}
