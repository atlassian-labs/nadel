package graphql.nadel.util

import java.time.Duration
import java.time.Instant

internal operator fun Instant.minus(other: Instant): Duration {
    return Duration.between(other, this)
}
