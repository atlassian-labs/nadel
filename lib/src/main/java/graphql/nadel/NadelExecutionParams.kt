package graphql.nadel

import graphql.nadel.time.NadelInternalLatencyTracker

class NadelExecutionParams internal constructor(
    val executionHints: NadelExecutionHints,
    val latencyTracker: NadelInternalLatencyTracker,
)
