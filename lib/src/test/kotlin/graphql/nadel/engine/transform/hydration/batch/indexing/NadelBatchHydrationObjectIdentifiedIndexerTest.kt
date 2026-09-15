package graphql.nadel.engine.transform.hydration.batch.indexing

import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.batch.NadelSharedResolvedObjectBatch
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.test.mock
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertEquals

class NadelBatchHydrationObjectIdentifiedIndexerTest {
    @Test
    fun `indexes a packed query from its aliased result path`() {
        val identifierAlias = "internal_identifier"
        val instruction = mock<NadelBatchHydrationFieldInstruction> { instruction ->
            every { instruction.queryPathToBackingField } returns NadelQueryPath(listOf("usersByIds"))
        }
        val aliasHelper = mock<NadelAliasHelper> { aliasHelper ->
            every { aliasHelper.getResultKey("id") } returns identifierAlias
        }
        val strategy = NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(
            listOf(
                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                    sourceIdCast = NadelObjectIdentifierCastingStrategy.NO_CAST,
                    sourceId = NadelQueryPath(listOf("id")),
                    resultId = "id",
                ),
            ),
        )
        val unaliasedObject = mutableMapOf<String, Any?>(
            identifierAlias to "wrong-user",
            "name" to "Wrong",
        )
        val aliasedObject = mutableMapOf<String, Any?>(
            identifierAlias to "wanted-user",
            "name" to "Wanted",
        )
        val result = NadelServiceExecutionResultImpl(
            data = linkedMapOf(
                "usersByIds" to mutableListOf(unaliasedObject),
                "shared_0" to mutableListOf(aliasedObject),
            ),
            errors = arrayListOf(),
        )

        val index = NadelBatchHydrationObjectIdentifiedIndexer(
            aliasHelper = aliasHelper,
            instruction = instruction,
            strategy = strategy,
        ).getSharedIndex(
            listOf(
                NadelSharedResolvedObjectBatch(
                    result = result,
                    resultPath = NadelQueryPath(listOf("shared_0")),
                ),
            ),
        )

        assertEquals(
            expected = mapOf("name" to "Wanted"),
            actual = index
                .getValue(NadelBatchHydrationIndexKey(listOf(JsonNode("wanted-user"))))
                .value,
        )
        assertEquals("wrong-user", unaliasedObject[identifierAlias])
    }
}
