package graphql.nadel.tests.util

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformState
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.tests.util.NadelTransformAdapter.State
import graphql.normalized.ExecutableNormalizedField

interface NadelTransformAdapter : NadelTransform<State> {
    object State : NadelTransformState

    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        return State
    }

    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}
