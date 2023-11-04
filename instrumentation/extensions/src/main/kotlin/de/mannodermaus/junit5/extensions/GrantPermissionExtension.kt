package de.mannodermaus.junit5.extensions

import android.Manifest
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.test.internal.platform.ServiceLoaderWrapper.loadSingleService
import androidx.test.internal.platform.content.PermissionGranter
import androidx.test.runner.permission.PermissionRequester
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * The [GrantPermissionExtension] allows granting of runtime permissions on Android M (API 23)
 * and above. Use this extension when a test requires a runtime permission to do its work.
 *
 * This is a port of JUnit 4's GrantPermissionRule for JUnit 5.
 *
 * <p>When applied to a test class it attempts to grant all requested runtime permissions.
 * The requested permissions will then be granted on the device and will take immediate effect.
 * Permissions can only be requested on Android M (API 23) or above and will be ignored on all other
 * API levels. Once a permission is granted it will apply for all tests running in the current
 * Instrumentation. There is no way of revoking a permission after it was granted. Attempting to do
 * so will crash the Instrumentation process.
 */
public class GrantPermissionExtension
internal constructor(private val permissionGranter: PermissionGranter) : BeforeEachCallback {

    public companion object {
        /**
         * Static factory method that grants the requested [permissions].
         *
         * <p>Permissions will be granted before any methods annotated with [BeforeEach] but before
         * any test method execution.
         *
         * @see android.Manifest.permission
         */
        @JvmStatic
        public fun grant(vararg permissions: String): GrantPermissionExtension {
            val granter = loadSingleService(PermissionGranter::class.java, ::PermissionRequester)

            return GrantPermissionExtension(granter).also {
                it.grantPermissions(permissions)
            }
        }

        private fun satisfyPermissionDependencies(permissions: Array<out String>): Set<String> {
            val set = LinkedHashSet<String>(permissions.size + 1).also { it.addAll(permissions) }

            // Grant READ_EXTERNAL_STORAGE implicitly if its counterpart is present
            if (Build.VERSION.SDK_INT >= 16 && Manifest.permission.WRITE_EXTERNAL_STORAGE in set) {
                set.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            return set
        }
    }

    internal fun grantPermissions(permissions: Array<out String>) {
        val permissionSet = satisfyPermissionDependencies(permissions)
        permissionGranter.addPermissions(*permissionSet.toTypedArray())
    }

    /* BeforeEachCallback */

    override fun beforeEach(context: ExtensionContext?) {
        permissionGranter.requestPermissions()
    }
}
