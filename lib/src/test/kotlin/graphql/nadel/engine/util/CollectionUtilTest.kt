package graphql.nadel.engine.util

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CollectionUtilTest {
    @Test
    fun `partitionCount counts number of values in each partition`() {
        val partition = listOf(1, 4, 6, 8, 10)

        val (first, second) = partition.partitionCount { it % 2 == 0 }

        assertTrue(first == 4)
        assertTrue(second == 1)
    }
}
