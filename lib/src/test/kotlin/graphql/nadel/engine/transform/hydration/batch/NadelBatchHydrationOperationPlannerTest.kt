package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.test.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NadelBatchHydrationOperationPlannerTest {
    @Test
    fun `keeps hook partitions in separate operations`() {
        val firstPartitionQuery = backingQuery(
            partitionOrdinal = 0,
            shardingTarget = "one-shard",
            sourceInput = "one",
        )
        val secondPartitionQuery = backingQuery(
            partitionOrdinal = 1,
            shardingTarget = "one-shard",
            sourceInput = "two",
        )

        val operations = NadelBatchHydrationOperationPlanner()
            .packBackingQueriesByExecutionBoundary(
                queries = listOf(firstPartitionQuery, secondPartitionQuery),
                maxCardinality = 2,
            )

        assertEquals(
            expected = listOf(
                listOf(firstPartitionQuery),
                listOf(secondPartitionQuery),
            ),
            actual = operations,
        )
    }

    @Test
    fun `keeps different sharding targets in separate operations`() {
        val firstShardQuery = backingQuery(
            partitionOrdinal = 0,
            shardingTarget = "first-shard",
            sourceInput = "one",
        )
        val sameShardQuery = backingQuery(
            partitionOrdinal = 0,
            shardingTarget = "first-shard",
            sourceInput = "two",
        )
        val secondShardQuery = backingQuery(
            partitionOrdinal = 0,
            shardingTarget = "second-shard",
            sourceInput = "three",
        )

        val operations = NadelBatchHydrationOperationPlanner()
            .packBackingQueriesByExecutionBoundary(
                queries = listOf(
                    firstShardQuery,
                    sameShardQuery,
                    secondShardQuery,
                ),
                maxCardinality = 3,
            )

        assertEquals(
            expected = listOf(
                listOf(firstShardQuery, sameShardQuery),
                listOf(secondShardQuery),
            ),
            actual = operations,
        )
    }

    @Test
    fun `packs consecutive queries by total batch cardinality`() {
        val operations = packByTotalCardinality(
            items = listOf(2, 3, 1, 4, 1),
            maxCardinality = 5,
            cardinality = { it },
        )

        assertEquals(
            expected = listOf(
                listOf(2, 3),
                listOf(1, 4),
                listOf(1),
            ),
            actual = operations,
        )
        assertTrue(
            operations.all { operation ->
                operation.sum() <= 5
            },
        )
    }

    @Test
    fun `preserves input order when splitting operations`() {
        val inputs = listOf(
            "first" to 4,
            "second" to 2,
            "third" to 3,
            "fourth" to 5,
        )

        val operations = packByTotalCardinality(
            items = inputs,
            maxCardinality = 5,
            cardinality = { (_, size) -> size },
        )

        assertEquals(
            expected = listOf(
                listOf("first"),
                listOf("second", "third"),
                listOf("fourth"),
            ),
            actual = operations.map { operation ->
                operation.map { (name) -> name }
            },
        )
    }

    @Test
    fun `returns no operations for no backing queries`() {
        assertEquals(
            expected = emptyList(),
            actual = packByTotalCardinality(
                items = emptyList<Int>(),
                maxCardinality = 5,
                cardinality = { it },
            ),
        )
    }

    @Test
    fun `rejects a query larger than the operation bound`() {
        assertFailsWith<IllegalArgumentException> {
            packByTotalCardinality(
                items = listOf(6),
                maxCardinality = 5,
                cardinality = { it },
            )
        }
    }

    @Test
    fun `rejects empty query batches`() {
        assertFailsWith<IllegalArgumentException> {
            packByTotalCardinality(
                items = listOf(0),
                maxCardinality = 5,
                cardinality = { it },
            )
        }
    }

    @Test
    fun `rejects non-positive operation bounds`() {
        assertFailsWith<IllegalArgumentException> {
            packByTotalCardinality(
                items = emptyList<Int>(),
                maxCardinality = 0,
                cardinality = { it },
            )
        }
    }

    @Test
    fun `partition validation permits reordered values`() {
        assertTrue(
            listOf(JsonNode("one"), JsonNode("two"), JsonNode("three"))
                .hasSameValuesAs(
                    listOf(JsonNode("three"), JsonNode("one"), JsonNode("two")),
                ),
        )
    }

    @Test
    fun `partition validation compares the complete value multiset`() {
        assertTrue(
            listOf(JsonNode("one"), JsonNode("one"), JsonNode("two"))
                .hasSameValuesAs(
                    listOf(JsonNode("two"), JsonNode("one"), JsonNode("one")),
                ),
        )
        assertFalse(
            listOf(JsonNode("one"), JsonNode("one"), JsonNode("two"))
                .hasSameValuesAs(
                    listOf(JsonNode("one"), JsonNode("two"), JsonNode("two")),
                ),
        )
    }

    private fun backingQuery(
        partitionOrdinal: Int,
        shardingTarget: Any?,
        sourceInput: Any,
    ): NadelBatchHydrationOperationPlanner.BackingQuery {
        return NadelBatchHydrationOperationPlanner.BackingQuery(
            lane = mock(relaxed = true),
            contributingConsumers = emptyList(),
            inputConsumers = emptyList(),
            batch = NadelHydrationArgumentsBatch(
                sourceInputs = listOf(JsonNode(sourceInput)),
                arguments = emptyMap(),
            ),
            partitionOrdinal = partitionOrdinal,
            field = mock(relaxed = true),
            resultPath = NadelQueryPath.root,
            shardingTarget = shardingTarget,
        )
    }
}
