package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.test.mock
import graphql.normalized.ExecutableNormalizedField
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NadelSharedBatchHydrationErrorMapperTest {
    @Test
    fun `routes an identified item error by root alias and fans out to matching source occurrences`() {
        val fixture = makeFixture()
        val legalClientAlias = "batch_hydrationName"
        val rawError = linkedMapOf<String, Any?>(
            "message" to "Name unavailable",
            "path" to listOf(fixture.primaryRootAlias, 0, legalClientAlias),
        )
        val result = NadelServiceExecutionResultImpl(
            data = linkedMapOf(
                fixture.primaryRootAlias to mutableListOf(
                    mutableMapOf<String, Any?>(
                        fixture.primaryIdentifierAlias to "user-1",
                        legalClientAlias to null,
                    ),
                ),
                fixture.secondaryRootAlias to mutableListOf(
                    mutableMapOf<String, Any?>(
                        fixture.secondaryIdentifierAlias to "user-2",
                        "displayName" to "Two",
                    ),
                ),
            ),
            errors = arrayListOf(rawError),
        )

        val outputs = NadelSharedBatchHydrationErrorMapper()
            .getOutputs(
                result = result,
                operation = fixture.operation,
            )

        assertEquals(setOf(17, 18), outputs.keys)
        assertFalse(19 in outputs)
        assertFalse(20 in outputs)

        val assigneeError = outputs.getValue(17).locatedErrors.single()
        assertTrue(outputs.getValue(17).instructions.isEmpty())
        assertSame(fixture.sourceObjects.getValue(17), assigneeError.subject)
        assertEquals(listOf("assignee", legalClientAlias), assigneeError.relativePath)
        assertEquals(rawError, assigneeError.rawError)

        val reporterError = outputs.getValue(18).locatedErrors.single()
        assertTrue(outputs.getValue(18).instructions.isEmpty())
        assertSame(fixture.sourceObjects.getValue(18), reporterError.subject)
        assertEquals(listOf("reporter", legalClientAlias), reporterError.relativePath)
        assertEquals(rawError, reporterError.rawError)
    }

    @Test
    fun `exposes an operation request error exactly once`() {
        val fixture = makeFixture()
        val rawError = linkedMapOf<String, Any?>(
            "message" to "Identity unavailable",
        )
        val result = NadelServiceExecutionResultImpl(
            data = linkedMapOf(
                fixture.primaryRootAlias to emptyList<Any?>(),
                fixture.secondaryRootAlias to emptyList<Any?>(),
            ),
            errors = arrayListOf(rawError),
        )

        val outputs = NadelSharedBatchHydrationErrorMapper()
            .getOutputs(
                result = result,
                operation = fixture.operation,
            )

        assertEquals(setOf(17), outputs.keys)
        val output = outputs.getValue(17)
        assertTrue(output.locatedErrors.isEmpty())
        val errorInstruction = assertIs<NadelResultInstruction.AddError>(
            output.instructions.single(),
        )
        assertEquals("Identity unavailable", errorInstruction.error.message)
        assertNull(errorInstruction.error.path)
    }

    @Test
    fun `confines an unattributable aliased query error to one contributing consumer`() {
        val fixture = makeFixture()
        val result = NadelServiceExecutionResultImpl(
            data = linkedMapOf(
                fixture.primaryRootAlias to emptyList<Any?>(),
                fixture.secondaryRootAlias to null,
            ),
            errors = arrayListOf(
                linkedMapOf(
                    "message" to "Secondary lookup unavailable",
                    "path" to listOf(fixture.secondaryRootAlias),
                ),
            ),
        )

        val outputs = NadelSharedBatchHydrationErrorMapper()
            .getOutputs(result = result, operation = fixture.operation)

        assertEquals(setOf(19), outputs.keys)
        val output = outputs.getValue(19)
        assertTrue(output.locatedErrors.isEmpty())
        val errorInstruction = assertIs<NadelResultInstruction.AddError>(
            output.instructions.single(),
        )
        assertEquals("Secondary lookup unavailable", errorInstruction.error.message)
        assertNull(errorInstruction.error.path)
    }

    @Test
    fun `does not expose an internal identifier alias`() {
        val fixture = makeFixture()
        val result = NadelServiceExecutionResultImpl(
            data = linkedMapOf(
                fixture.primaryRootAlias to mutableListOf(
                    mutableMapOf<String, Any?>(
                        fixture.primaryIdentifierAlias to null,
                    ),
                ),
                fixture.secondaryRootAlias to emptyList<Any?>(),
            ),
            errors = arrayListOf(
                linkedMapOf(
                    "message" to "Identifier unavailable",
                    "path" to listOf(
                        fixture.primaryRootAlias,
                        0,
                        fixture.primaryIdentifierAlias,
                    ),
                ),
            ),
        )

        val outputs = NadelSharedBatchHydrationErrorMapper()
            .getOutputs(result = result, operation = fixture.operation)

        assertEquals(setOf(17), outputs.keys)
        val output = outputs.getValue(17)
        assertTrue(output.locatedErrors.isEmpty())
        val errorInstruction = assertIs<NadelResultInstruction.AddError>(
            output.instructions.single(),
        )
        assertNull(errorInstruction.error.path)
    }

    private fun makeFixture(): Fixture {
        val objectIdentifier = NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
            sourceIdCast = NadelObjectIdentifierCastingStrategy.NO_CAST,
            sourceId = NadelQueryPath(listOf("id")),
            resultId = "id",
        )
        val matchStrategy = NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(
            listOf(objectIdentifier),
        )
        val instruction = mock<NadelBatchHydrationFieldInstruction> { instruction ->
            every { instruction.batchHydrationMatchStrategy } returns matchStrategy
        }

        fun makeConsumer(stableId: Int): NadelBatchHydrationCoalescingConsumer {
            return checkNotNull(
                NadelBatchHydrationCoalescingConsumer.createOrNull(
                    stableId = stableId,
                    hydration = mock(),
                    instruction = instruction,
                ),
            )
        }

        val consumers = listOf(17, 18, 19, 20).associateWith(::makeConsumer)
        val sourceObjects = consumers.mapValues { (stableId) ->
            JsonNode(
                mutableMapOf<String, Any?>(
                    "source" to stableId,
                ),
            )
        }
        val primaryIdentifierAlias = "primary_internal_identifier"
        val secondaryIdentifierAlias = "secondary_internal_identifier"
        val primaryRootAlias = "batch_hydration__0_0"
        val secondaryRootAlias = "batch_hydration__0_1"
        val primaryConsumers = listOf(
            inputConsumer(17, "user-1", sourceObjects, "assignee"),
            inputConsumer(18, "user-1", sourceObjects, "reporter"),
            inputConsumer(20, "user-3", sourceObjects, "watcher"),
        )
        val secondaryConsumers = listOf(
            inputConsumer(19, "user-2", sourceObjects, "owner"),
        )
        val primaryLane = selectionLane(
            consumers = listOf(
                consumers.getValue(17),
                consumers.getValue(18),
                consumers.getValue(20),
            ),
            identifierAlias = primaryIdentifierAlias,
        )
        val secondaryLane = selectionLane(
            consumers = listOf(consumers.getValue(19)),
            identifierAlias = secondaryIdentifierAlias,
        )
        val primaryQuery = backingQuery(
            lane = primaryLane,
            contributingConsumers = primaryLane.consumers,
            inputConsumers = primaryConsumers,
            rootAlias = primaryRootAlias,
        )
        val secondaryQuery = backingQuery(
            lane = secondaryLane,
            contributingConsumers = secondaryLane.consumers,
            inputConsumers = secondaryConsumers,
            rootAlias = secondaryRootAlias,
        )

        return Fixture(
            operation = NadelBatchHydrationOperationPlanner.Operation(
                queries = listOf(primaryQuery, secondaryQuery),
            ),
            sourceObjects = sourceObjects,
            primaryIdentifierAlias = primaryIdentifierAlias,
            secondaryIdentifierAlias = secondaryIdentifierAlias,
            primaryRootAlias = primaryRootAlias,
            secondaryRootAlias = secondaryRootAlias,
        )
    }

    private fun inputConsumer(
        stableId: Int,
        userId: String,
        sourceObjects: Map<Int, JsonNode>,
        resultKey: String,
    ): NadelBatchHydrationInputConsumer {
        return NadelBatchHydrationInputConsumer(
            stableId = stableId,
            indexKey = NadelBatchHydrationIndexKey(listOf(JsonNode(userId))),
            sourceObject = sourceObjects.getValue(stableId),
            relativePath = listOf(resultKey),
        )
    }

    private fun selectionLane(
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
        identifierAlias: String,
    ): NadelBatchHydrationOperationPlanner.SelectionLane {
        val aliasHelper = mock<NadelAliasHelper> { aliasHelper ->
            every { aliasHelper.getResultKey("id") } returns identifierAlias
        }
        return NadelBatchHydrationOperationPlanner.SelectionLane(
            consumers = consumers,
            aliasHelper = aliasHelper,
            batches = emptyList(),
            inputConsumersBySourceInput = emptyMap(),
        )
    }

    private fun backingQuery(
        lane: NadelBatchHydrationOperationPlanner.SelectionLane,
        contributingConsumers: List<NadelBatchHydrationCoalescingConsumer>,
        inputConsumers: List<NadelBatchHydrationInputConsumer>,
        rootAlias: String,
    ): NadelBatchHydrationOperationPlanner.BackingQuery {
        val backingField = mock<ExecutableNormalizedField> { field ->
            every { field.resultKey } returns rootAlias
        }
        return NadelBatchHydrationOperationPlanner.BackingQuery(
            lane = lane,
            contributingConsumers = contributingConsumers,
            inputConsumers = inputConsumers,
            batch = mock(relaxed = true),
            partitionOrdinal = 0,
            field = backingField,
            resultPath = NadelQueryPath(listOf(rootAlias)),
            shardingTarget = null,
        )
    }

    private data class Fixture(
        val operation: NadelBatchHydrationOperationPlanner.Operation,
        val sourceObjects: Map<Int, JsonNode>,
        val primaryIdentifierAlias: String,
        val secondaryIdentifierAlias: String,
        val primaryRootAlias: String,
        val secondaryRootAlias: String,
    )
}
