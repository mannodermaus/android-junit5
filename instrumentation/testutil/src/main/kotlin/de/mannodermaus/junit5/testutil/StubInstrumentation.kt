package de.mannodermaus.junit5.testutil

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.Context
import android.content.res.Resources
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock

internal class StubInstrumentation : Instrumentation() {
    private val targetContext = createMockTargetContext()
    private val context = createMockContext()

    override fun getTargetContext(): Context {
        return targetContext
    }

    override fun getContext(): Context {
        return context
    }

    companion object {
        private fun createMockTargetContext(): Context {
            return mock {
                // Needed for some ExecutionCondition tests that
                // require access to the BuildConfig class inside module ':core'
                on { packageName } doReturn "de.mannodermaus.junit5.util"
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun createMockContext(): Context {
            // Needed by AndroidJUnit5Tests in module ':runner'
            val resources =
                mock<Resources> {
                    on { getIdentifier(any(), any(), any()) } doReturn 0
                    on { openRawResource(any()) } doThrow Resources.NotFoundException("mocked call")
                }

            return mock {
                on { packageName } doReturn "de.mannodermaus.junit5.instrumentation"
                on { getResources() } doReturn resources
            }
        }
    }
}
