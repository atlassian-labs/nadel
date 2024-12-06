package graphql.nadel.time

import java.time.Duration

interface NadelInternalLatencyTracker {
    /**
     * Gets the _current_ internal latency.
     *
     * This can be invoked before the latency is completely tracked to get a running track
     * of latency.
     */
    fun getInternalLatency(): Duration
}

