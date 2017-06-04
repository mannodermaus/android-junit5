package de.mannodermaus.gradle.anj5

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification

/**
 * Test the behavior of AndroidJUnit5Compat methods on different versions of the AGP.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class AndroidJUnit5CompatSpec extends Specification {

    MockedVariantScope mockedScope

    def setup() {
        mockedScope = new MockedVariantScope()
    }

    def "Use javaOuptuts below AGP 2.2.0"() {
        when:
        def versions = [
                "2.0.0-alpha1",
                "2.0.0-beta2",
                "2.0.0-rc1",
                "2.0.0",
                "2.1.0-alpha1",
                "2.1.0-beta1",
                "2.1.0-rc1",
                "2.1.0",
                "2.1.2",
                "2.1.3",
        ]

        versions.forEach {
            AndroidJUnit5Compat.getJavaOutputDirs(Version.valueOf(it), mockedScope)
        }

        then:
        assert mockedScope.calledJavaOuptuts == versions.size()
        assert mockedScope.calledJavaOutputs == 0
        assert mockedScope.calledJavaOutputDir == 0
        assert mockedScope.calledAnnotationProcessorOutputDir == 0
    }

    def "Use javaOutputs below AGP 3.0.0"() {
        when:
        def versions = [
                "2.2.0-alpha1",
                "2.2.0-beta1",
                "2.2.0-rc1",
                "2.2.0",
                "2.2.1",
                "2.2.2",
                "2.2.3",
                "2.3.0-alpha1",
                "2.3.0-beta1",
                "2.3.0-rc1",
                "2.3.0",
                "2.4.0-alpha1",
        ]

        versions.forEach {
            AndroidJUnit5Compat.getJavaOutputDirs(Version.valueOf(it), mockedScope)
        }

        then:
        assert mockedScope.calledJavaOuptuts == 0
        assert mockedScope.calledJavaOutputs == versions.size()
        assert mockedScope.calledJavaOutputDir == 0
        assert mockedScope.calledAnnotationProcessorOutputDir == 0
    }

    def "Use separated outputs after AGP 3.0.0"() {
        when:
        def versions = [
                "3.0.0-alpha1",
                "3.0.0-alpha2",
        ]

        versions.forEach {
            AndroidJUnit5Compat.getJavaOutputDirs(Version.valueOf(it), mockedScope)
        }

        then:
        assert mockedScope.calledJavaOuptuts == 0
        assert mockedScope.calledJavaOutputs == 0
        assert mockedScope.calledJavaOutputDir == versions.size()
        assert mockedScope.calledAnnotationProcessorOutputDir == versions.size()
    }

    /* Inner classes & mocks */

    /**
     * VariantScope mock, which simulates
     * all different versions of the VariantScope interface relevant to the plugin,
     * and simply counts the invocations of its methods.
     */
    static final class MockedVariantScope {

        private int calledJavaOutputs = 0
        private int calledJavaOuptuts = 0

        private int calledJavaOutputDir = 0
        private int calledAnnotationProcessorOutputDir = 0

        MockedVariantScope() {
        }

        Iterable<File> getJavaOuptuts() {
            calledJavaOuptuts++
            return []
        }

        Iterable<File> getJavaOutputs() {
            calledJavaOutputs++
            return []
        }

        File getJavaOutputDir() {
            calledJavaOutputDir++
            return new File("")
        }

        File getAnnotationProcessorOutputDir() {
            calledAnnotationProcessorOutputDir++
            return new File("")
        }
    }
}
