package de.mannodermaus.gradle.plugins.junit5.internal.extensions

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

internal fun Logger.agpLog(level: LogLevel, message: String) {
    val pair: Pair<String, (String) -> Unit> = when (level) {
        LogLevel.ERROR -> "error" to { s -> error(s) }
        LogLevel.WARN -> "warning" to { s -> warn(s) }
        LogLevel.INFO -> "info" to { s -> info(s) }
        else -> "debug" to { s -> debug(s) }
    }
    val (kind, log) = pair
    log("""AGBPI: {"kind": "$kind","text":"$message"}""")
}

internal fun Logger.junit5Info(text: String) {
    info("[android-junit5]: $text")
}

internal fun Logger.junit5Warn(text: String) {
    warn("[android-junit5]: $text")
}
