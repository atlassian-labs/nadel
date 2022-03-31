package graphql.nadel.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal object LogKit {
    inline fun <reified T> getLogger(): Logger {
        return LoggerFactory.getLogger(T::class.java)
    }
}
