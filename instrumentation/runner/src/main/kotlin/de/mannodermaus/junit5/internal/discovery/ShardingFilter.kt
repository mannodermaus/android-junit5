package de.mannodermaus.junit5.internal.discovery

import android.os.Bundle
import de.mannodermaus.junit5.internal.extensions.isDynamicTest
import kotlin.math.abs
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.PostDiscoveryFilter
import org.junit.platform.launcher.TestIdentifier

/**
 * JUnit 5 implementation of the default instrumentation's
 * `androidx.test.internal.runner.TestRequestBuilder$ShardingFilter`, ported to the new API to
 * support dynamic test templates, too.
 *
 * Based on a draft by KyoungJoo Jeon (@jkj8790).
 */
internal class ShardingFilter(private val numShards: Int, private val shardIndex: Int) :
    PostDiscoveryFilter {

    companion object {
        private const val ARG_NUM_SHARDS = "numShards"
        private const val ARG_SHARD_INDEX = "shardIndex"

        fun fromArguments(arguments: Bundle): ShardingFilter? {
            val numShards = arguments.getString(ARG_NUM_SHARDS)?.toInt() ?: -1
            val shardIndex = arguments.getString(ARG_SHARD_INDEX)?.toInt() ?: -1

            return if (numShards > 0 && shardIndex >= 0 && shardIndex < numShards) {
                ShardingFilter(numShards, shardIndex)
            } else {
                null
            }
        }
    }

    override fun apply(descriptor: TestDescriptor): FilterResult {
        val identifier = TestIdentifier.from(descriptor)

        if (identifier.isTest || identifier.isDynamicTest) {
            val remainder = abs(identifier.hashCode()) % numShards
            return if (remainder == shardIndex) {
                FilterResult.included(null)
            } else {
                FilterResult.excluded("excluded")
            }
        }

        return FilterResult.included(null)
    }
}
