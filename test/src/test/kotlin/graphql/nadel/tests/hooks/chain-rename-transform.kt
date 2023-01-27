package graphql.nadel.tests.hooks

import graphql.language.EnumValue
import graphql.language.StringValue
import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformState
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

private class ChainRenameTransform : NadelTransform<ChainRenameTransform.State> {
    object State : NadelTransformState

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        return State.takeIf { overallField.name == "test" || overallField.name == "cities" }
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        service: Service,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        if (field.normalizedArguments["arg"] != null) {
            return NadelTransformFieldResult(
                newField = field.toBuilder()
                    .normalizedArguments(field.normalizedArguments.let {
                        it + ("arg" to NormalizedInputValue("String", StringValue("aaarrg")))
                    })
                    .build(),
            )
        }

        if (field.normalizedArguments["continent"] != null) {
            return NadelTransformFieldResult(
                newField = field.toBuilder()
                    .normalizedArguments(field.normalizedArguments.let {
                        it + ("continent" to NormalizedInputValue("Continent", EnumValue("Asia")))
                    })
                    .build(),
            )
        }

        error("Did not match transform conditions")
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun getResultInstructions(
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}

@UseHook
class `chain-rename-transform` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out NadelTransformState>> = listOf(
        ChainRenameTransform(),
    )
}

@UseHook
class `chain-rename-transform-with-type-rename` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out NadelTransformState>> = listOf(
        ChainRenameTransform(),
    )
}
