package de.mannodermaus.junit5.extensions

import android.Manifest
import android.os.Build
import androidx.test.internal.platform.content.PermissionGranter
import com.google.common.truth.Truth.assertThat
import de.mannodermaus.junit5.testutil.AndroidBuildUtils.withApiLevel
import java.lang.reflect.Modifier
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.Mockito.mock

class GrantPermissionExtensionTests {

    private val granter = TestPermissionGranter()

    @Test
    fun `single permission`() {
        runExtension(Manifest.permission.CAMERA)

        assertThat(granter.grantedPermissions).containsExactly(Manifest.permission.CAMERA)
    }

    @Test
    fun `multiple permissions`() {
        runExtension(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        assertThat(granter.grantedPermissions)
            .containsExactly(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            .inOrder()
    }

    @TestFactory
    fun `implicit addition of READ_EXTERNAL_STORAGE`(): List<DynamicTest> {
        // Run this test for every available Android OS version.
        val latestApi = findLatestAndroidApiLevel()

        return (26..latestApi).map { api ->
            dynamicTest("API $api") {
                withApiLevel(api) {
                    runExtension(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                    assertThat(granter.grantedPermissions)
                        .containsExactly(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                        )
                        .inOrder()
                }
            }
        }
    }

    /* Private */

    private fun findLatestAndroidApiLevel(): Int {
        // Look inside Build.VERSION_CODES and locate
        // the static int field with the highest value,
        // except for the 'CUR_DEVELOPMENT' test field
        return Build.VERSION_CODES::class
            .java
            .declaredFields
            .filter { Modifier.isStatic(it.modifiers) }
            .filter { it.type == Int::class.java }
            .filter { it.name != "CUR_DEVELOPMENT" }
            .maxOf { it.get(null) as Int }
    }

    private fun runExtension(vararg permissions: String) {
        val extension = GrantPermissionExtension(granter)
        val context = mock<ExtensionContext>()

        extension.grantPermissions(permissions)
        extension.beforeEach(context)
    }

    private class TestPermissionGranter : PermissionGranter {
        private val pending = mutableSetOf<String>()
        private val granted = mutableSetOf<String>()

        override fun addPermissions(vararg permissions: String) {
            pending.addAll(permissions)
        }

        override fun requestPermissions() {
            granted.addAll(pending)
            pending.clear()
        }

        val grantedPermissions: Set<String> = granted
    }
}
