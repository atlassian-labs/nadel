package graphql.nadel.tests.hooks

import graphql.language.EnumValue
import graphql.language.StringValue
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

private class ChainRenameTransform : NadelTransform<Any> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): Any? {
        return overallField.takeIf { it.name == "test" || it.name == "cities" }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Any,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
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

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: Any,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}

@UseHook
class `chain-rename-transform` : EngineTestHook {
    override val customTransforms: List<NadelTransform<Any>> = listOf(
        ChainRenameTransform(),
    )
}

@UseHook
class `chain-rename-transform-with-type-rename` : EngineTestHook {
    override val customTransforms: List<NadelTransform<Any>> = listOf(
        ChainRenameTransform(),
    )
}
