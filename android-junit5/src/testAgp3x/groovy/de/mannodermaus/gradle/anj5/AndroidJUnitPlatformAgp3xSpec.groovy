package de.mannodermaus.gradle.anj5

import org.gradle.api.Project
import org.gradle.internal.resolve.ArtifactResolveException
import org.gradle.internal.resolve.ModuleVersionNotFoundException
import org.gradle.testfixtures.ProjectBuilder

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
