package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.NadelResultMutation
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.NadelResultOccurrence
import graphql.nadel.test.mock
import graphql.normalized.ExecutableNormalizedField
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class NadelSharedBatchHydrationErrorMapperTest {
    @Test
    fun `fans out a null-bubbled error while preserving a legal internal-prefix alias`() {
        val invocation = mock<NadelNewBatchHydrator.Invocation> { invocation ->
            every { invocation.id } returns 17
        }
        val secondInvocation = mock<NadelNewBatchHydrator.Invocation> { mockInvocation ->
            every { mockInvocation.id } returns 18
        }
        val matchStrategy = NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(
            listOf(
                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                    sourceIdCast = NadelObjectIdentifierCastingStrategy.NO_CAST,
                    sourceId = NadelQueryPath(listOf("id")),
                    resultId = "id",
                ),
            ),
        )
        val instruction = mock<NadelBatchHydrationFieldInstruction> { instruction ->
            every { instruction.batchHydrationMatchStrategy } returns matchStrategy
        }
        val hydration = mock<NadelNewBatchHydrator.PreparedBatchHydration> { hydration ->
            every { hydration.invocation } returns invocation
        }
        val secondHydration = mock<NadelNewBatchHydrator.PreparedBatchHydration> { mockHydration ->
            every { mockHydration.invocation } returns secondInvocation
        }
        val consumer = checkNotNull(
            NadelBatchHydrationCoalescingConsumer.createOrNull(
                hydration = hydration,
                instruction = instruction,
            ),
        )
        val secondConsumer = checkNotNull(
            NadelBatchHydrationCoalescingConsumer.createOrNull(
                hydration = secondHydration,
                instruction = instruction,
            ),
        )
        val sourceOccurrence = NadelResultOccurrence(
            node = JsonNode(mutableMapOf<String, Any?>()),
        )
        val secondSourceOccurrence = NadelResultOccurrence(
            node = JsonNode(mutableMapOf<String, Any?>()),
        )
        val inputConsumer = NadelBatchHydrationInputConsumer(
            invocationId = invocation.id,
            indexKey = NadelBatchHydrationIndexKey(JsonNode("user-1")),
            sourceOccurrence = sourceOccurrence,
            relativePath = listOf("assignee"),
        )
        val secondInputConsumer = NadelBatchHydrationInputConsumer(
            invocationId = secondInvocation.id,
            indexKey = NadelBatchHydrationIndexKey(JsonNode("user-2")),
            sourceOccurrence = secondSourceOccurrence,
            relativePath = listOf("reporter"),
        )
        val internalRootAlias = "batch_hydration__0_0"
        val legalClientAlias = "batch_hydrationName"
        val backingField = mock<ExecutableNormalizedField> { field ->
            every { field.resultKey } returns internalRootAlias
        }
        val aliasHelper = mock<NadelAliasHelper> { aliasHelper ->
            every { aliasHelper.getResultKey("id") } returns "internal_identifier_alias"
        }
        val lane = NadelBatchHydrationOperationPlanner.SelectionLane(
            consumers = listOf(consumer, secondConsumer),
            aliasHelper = aliasHelper,
            batches = emptyList(),
            inputConsumersBySourceInput = emptyMap(),
        )
        val query = NadelBatchHydrationOperationPlanner.BackingQuery(
            lane = lane,
            contributingConsumers = listOf(consumer, secondConsumer),
            inputConsumers = listOf(inputConsumer, secondInputConsumer),
            batch = mock(relaxed = true),
            field = backingField,
            resultPath = NadelQueryPath(listOf(internalRootAlias)),
            shardingTarget = null,
        )
        val operation = NadelBatchHydrationOperationPlanner.Operation(
            queries = listOf(query),
            consumers = listOf(consumer, secondConsumer),
        )
        val rawError = linkedMapOf<String, Any?>(
            "message" to "Name unavailable",
            "path" to listOf(internalRootAlias, 0, legalClientAlias),
        )
        val result = NadelServiceExecutionResultImpl(
            data = linkedMapOf(
                internalRootAlias to mutableListOf<Any?>(null),
            ),
            errors = arrayListOf(rawError),
        )

        val mutations = NadelSharedBatchHydrationErrorMapper()
            .getMutations(
                result = result,
                operation = operation,
            )

        val errorInstruction = assertIs<NadelResultMutation.AddErrorAt>(
            mutations.getValue(invocation.id).single(),
        )
        assertSame(sourceOccurrence, errorInstruction.subject)
        assertEquals(
            expected = listOf("assignee", legalClientAlias),
            actual = errorInstruction.relativePath,
        )
        assertEquals(rawError, errorInstruction.rawError)

        val secondErrorInstruction = assertIs<NadelResultMutation.AddErrorAt>(
            mutations.getValue(secondInvocation.id).single(),
        )
        assertSame(secondSourceOccurrence, secondErrorInstruction.subject)
        assertEquals(
            expected = listOf("reporter", legalClientAlias),
            actual = secondErrorInstruction.relativePath,
        )
        assertEquals(rawError, secondErrorInstruction.rawError)
    }
}
