package de.mannodermaus.gradle.plugins.junit5

import com.android.build.gradle.api.BaseVariant
import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationKind
import de.mannodermaus.gradle.plugins.junit5.internal.ConfigurationScope
import de.mannodermaus.gradle.plugins.junit5.internal.find
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Configuration
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.api.lifecycle.CachingMode.SCOPE
import org.mockito.Mockito

/**
 * Unit Tests related to the plugin's extension functions,
 * used to validate internal behavior augmenting existing functionality.
 */
class ExtensionSpec : Spek({

  describe("tests for ConfigurationContainer#find()") {

    // Quick-hand mock creator for Configurations
    fun mockConfiguration(name: String): Configuration =
        Mockito.mock(Configuration::class.java).apply {
          Mockito.`when`(this.name).thenReturn(name)
        }

    fun mockVariant(name: String): BaseVariant =
        Mockito.mock(BaseVariant::class.java).apply {
          Mockito.`when`(this.name).thenReturn(name)
        }

    val debugVariant by memoized(SCOPE) { mockVariant("debug") }

    // All different kinds of configurations
    val api by memoized(SCOPE) { mockConfiguration("api") }
    val testApi by memoized(SCOPE) { mockConfiguration("testApi") }
    val androidTestApi by memoized(SCOPE) { mockConfiguration("androidTestApi") }
    val debugApi by memoized(SCOPE) { mockConfiguration("debugApi") }
    val debugTestApi by memoized(SCOPE) { mockConfiguration("debugTestApi") }
    val debugAndroidTestApi by memoized(SCOPE) { mockConfiguration("debugAndroidTestApi") }
    val impl by memoized(SCOPE) { mockConfiguration("implementation") }
    val testImpl by memoized(SCOPE) { mockConfiguration("testImplementation") }
    val androidTestImpl by memoized(SCOPE) { mockConfiguration("androidTestImplementation") }
    val debugImpl by memoized(SCOPE) { mockConfiguration("debugImplementation") }
    val debugTestImpl by memoized(SCOPE) { mockConfiguration("debugTestImplementation") }
    val debugAndroidTestImpl by memoized(SCOPE) {
      mockConfiguration("debugAndroidTestImplementation")
    }
    val compOnly by memoized(SCOPE) { mockConfiguration("compileOnly") }
    val testCompOnly by memoized(SCOPE) { mockConfiguration("testCompileOnly") }
    val androidTestCompOnly by memoized(SCOPE) { mockConfiguration("androidTestCompileOnly") }
    val debugCompOnly by memoized(SCOPE) { mockConfiguration("debugCompileOnly") }
    val debugTestCompOnly by memoized(SCOPE) { mockConfiguration("debugTestCompileOnly") }
    val debugAndroidTestCompOnly by memoized(SCOPE) {
      mockConfiguration("debugAndroidTestCompileOnly")
    }
    val runtOnly by memoized(SCOPE) { mockConfiguration("runtimeOnly") }
    val testRuntOnly by memoized(SCOPE) { mockConfiguration("testRuntimeOnly") }
    val androidTestRuntOnly by memoized(SCOPE) { mockConfiguration("androidTestRuntimeOnly") }
    val debugRuntOnly by memoized(SCOPE) { mockConfiguration("debugRuntimeOnly") }
    val debugTestRuntOnly by memoized(SCOPE) { mockConfiguration("debugTestRuntimeOnly") }
    val debugAndroidTestRuntOnly by memoized(SCOPE) {
      mockConfiguration("debugAndroidTestRuntimeOnly")
    }

    // Theoretical container of all Configurations
    val container by memoized(SCOPE) {
      setOf(api, impl, compOnly, runtOnly,
          debugApi, debugImpl, debugCompOnly, debugRuntOnly,
          testApi, testImpl, testCompOnly, testRuntOnly,
          debugTestApi, debugTestImpl, debugTestCompOnly, debugTestRuntOnly,
          androidTestApi, androidTestImpl, androidTestCompOnly, androidTestRuntOnly,
          debugAndroidTestApi, debugAndroidTestImpl, debugAndroidTestCompOnly,
          debugAndroidTestRuntOnly)
    }

    // All different kinds of combinations are exercised
    mapOf(
        // "Variant-less configurations"
        null to mapOf(
            ConfigurationScope.API to listOf(
                ConfigurationKind.APP to api,
                ConfigurationKind.TEST to testApi,
                ConfigurationKind.ANDROID_TEST to androidTestApi
            ),
            ConfigurationScope.IMPLEMENTATION to listOf(
                ConfigurationKind.APP to impl,
                ConfigurationKind.TEST to testImpl,
                ConfigurationKind.ANDROID_TEST to androidTestImpl
            ),
            ConfigurationScope.COMPILE_ONLY to listOf(
                ConfigurationKind.APP to compOnly,
                ConfigurationKind.TEST to testCompOnly,
                ConfigurationKind.ANDROID_TEST to androidTestCompOnly
            ),
            ConfigurationScope.RUNTIME_ONLY to listOf(
                ConfigurationKind.APP to runtOnly,
                ConfigurationKind.TEST to testRuntOnly,
                ConfigurationKind.ANDROID_TEST to androidTestRuntOnly
            )
        ),
        // "Variant-aware configurations"
        debugVariant to mapOf(
            ConfigurationScope.API to listOf(
                ConfigurationKind.APP to debugApi,
                ConfigurationKind.TEST to debugTestApi,
                ConfigurationKind.ANDROID_TEST to debugAndroidTestApi
            ),
            ConfigurationScope.IMPLEMENTATION to listOf(
                ConfigurationKind.APP to debugImpl,
                ConfigurationKind.TEST to debugTestImpl,
                ConfigurationKind.ANDROID_TEST to debugAndroidTestImpl
            ),
            ConfigurationScope.COMPILE_ONLY to listOf(
                ConfigurationKind.APP to debugCompOnly,
                ConfigurationKind.TEST to debugTestCompOnly,
                ConfigurationKind.ANDROID_TEST to debugAndroidTestCompOnly
            ),
            ConfigurationScope.RUNTIME_ONLY to listOf(
                ConfigurationKind.APP to debugRuntOnly,
                ConfigurationKind.TEST to debugTestRuntOnly,
                ConfigurationKind.ANDROID_TEST to debugAndroidTestRuntOnly
            )
        )
    ).forEach { variant, scopes ->
      val variantDescription = if (variant != null) {
        "variant-aware '${variant.name}' configurations"
      } else {
        "variant-less configurations"
      }

      context("validation of $variantDescription") {
        scopes.forEach { scope, pairs ->

          on("using scope == $scope") {
            pairs.forEach { (kind, expected) ->

              it("finds configuration '${expected.name}' for kind == $kind") {
                assertThat(container.find(
                    variant = variant,
                    kind = kind,
                    scope = scope))
                    .isEqualTo(expected)
              }
            }
          }
        }
      }
    }
  }
})
