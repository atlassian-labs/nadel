@file:JvmName("LogKt")

package graphql.nadel.enginekt.log

import graphql.nadel.util.LogKit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal inline fun <reified T> getLogger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

internal inline fun <reified T> getNotPrivacySafeLogger(): Logger {
    return LogKit.getNotPrivacySafeLogger(T::class.java)
}

inline fun Logger.ifTraceEnabled(logFunc: Logger.() -> Unit) {
    if (isTraceEnabled) {
        logFunc()
    }
}

inline fun Logger.ifDebugEnabled(logFunc: Logger.() -> Unit) {
    if (isDebugEnabled) {
        logFunc()
    }
}

inline fun Logger.ifInfoEnabled(logFunc: Logger.() -> Unit) {
    if (isInfoEnabled) {
        logFunc()
    }
}

inline fun Logger.ifWarnEnabled(logFunc: Logger.() -> Unit) {
    if (isWarnEnabled) {
        logFunc()
    }
}

inline fun Logger.ifErrorEnabled(logFunc: Logger.() -> Unit) {
    if (isErrorEnabled) {
        logFunc()
    }
}

var kvImpl: (String, Any?) -> Any? = { key, value ->
    key to value
}

internal fun Logger.kv(key: String, value: Any?): Any? {
    return kvImpl(key, value)
}
