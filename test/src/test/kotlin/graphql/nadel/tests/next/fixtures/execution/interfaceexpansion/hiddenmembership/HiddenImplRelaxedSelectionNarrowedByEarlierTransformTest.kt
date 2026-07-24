package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.hiddenmembership

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.tests.util.NadelTransformAdapter
import graphql.normalized.ExecutableNormalizedField

/**
 * Simulates an earlier transform narrowing an abstract selection to one implementation. The
 * [NadelNoInterfaceToObjectFragmentExpansionTransform] transform must re-evaluate the narrowed field instead of
 * applying its precomputed plan: should keep the fragments on interfaces if they were added by previous transforms.
 */
class HiddenImplRelaxedSelectionNarrowedByEarlierTransformTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .transforms(listOf(NarrowIdSelectionToIssueTransform()))
    }
}

private class NarrowIdSelectionToIssueTransform : NadelTransformAdapter {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): Unit? {
        return Unit.takeIf {
            overallField.name == "id" &&
                overallField.objectTypeNames.toSet() == setOf("Issue", "Story")
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Unit,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = field.toBuilder()
                .clearObjectTypesNames()
                .objectTypeNames(listOf("Issue"))
                .build(),
        )
    }
}
