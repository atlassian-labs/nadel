package graphql.nadel.time

import java.time.Duration

/**
 * This calculates the time elapsed while considering that times can overlap.
 *
 * Timings are submitted to [submit] as soon as they finish i.e. earliest end first
 */
internal class NadelParallelElapsedCalculator {
    /**
     * Effectively a LinkedList
     */
    private data class TimeNode(
        val prev: TimeNode?,
        @Volatile
        var start: Duration,
        @Volatile
        var end: Duration,
        @Volatile
        var next: TimeNode?,
    )

    @Volatile
    private var tail: TimeNode? = null

    @Synchronized
    fun submit(start: Duration, end: Duration) {
        val headOverlapNode = getHeadOverlapNode(start)

        if (headOverlapNode == null) {
            val currentTail = tail
            val newTail = TimeNode(
                prev = currentTail,
                start,
                end,
                next = null,
            )
            currentTail?.next = newTail
            tail = newTail
        } else {
            headOverlapNode.start = min(start, headOverlapNode.start)
            headOverlapNode.end = max(end, headOverlapNode.end)
            headOverlapNode.next = null
            tail = headOverlapNode
        }
    }

    fun calculate(): Duration {
        var sum = 0L

        var cursor = tail
        while (cursor != null) {
            sum += cursor.end.toNanos() - cursor.start.toNanos()
            cursor = cursor.prev
        }

        return Duration.ofNanos(sum)
    }

    /**
     * We are finding the [TimeNode] closest to the head that still overlaps.
     */
    private fun getHeadOverlapNode(
        start: Duration,
    ): TimeNode? {
        var best: TimeNode? = null

        var cursor = tail
        while (cursor != null) {
            if (start < cursor.end) {
                best = cursor
            } else {
                break
            }
            cursor = cursor.prev
        }

        return best
    }

    private fun min(a: Duration, b: Duration): Duration {
        return if (a < b) a else b
    }

    private fun max(a: Duration, b: Duration): Duration {
        return if (a > b) a else b
    }
}
