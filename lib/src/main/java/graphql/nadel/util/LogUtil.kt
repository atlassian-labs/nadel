package graphql.nadel.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> getLogger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

/**
 * Creates a logger with a name indicating that the content might not be privacy safe
 * eg it could contain user generated content or privacy information.
 *
 * @param clazz the class to make a logger for
 *
 * @return a new Logger
 */
inline fun <reified T> getNotPrivacySafeLogger(): Logger {
    return LoggerFactory.getLogger("notprivacysafe." + T::class.java.name)
}
