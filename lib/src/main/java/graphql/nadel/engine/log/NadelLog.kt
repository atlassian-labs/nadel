package graphql.nadel.engine.log

import org.slf4j.Logger

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
