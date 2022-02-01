package graphql.nadel.tests.hooks

import graphql.language.StringValue
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.getField
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.toBuilder
import graphql.nadel.enginekt.util.toMapStrictly
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

private class AriTestTransform : NadelTransform<Set<String>> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): Set<String>? {
        // Let's not bother with abstract types in a test
        val fieldCoords = makeFieldCoordinates(overallField.objectTypeNames.single(), overallField.name)
        val fieldDef = executionBlueprint.schema.getField(fieldCoords)
            ?: error("Unable to fetch field definition")
        val fieldArgDefs = fieldDef.arguments.strictAssociateBy { it.name }

        return overallField.normalizedArguments.keys
            .asSequence()
            .filter {
                fieldArgDefs[it]?.hasDirective("interpretAri") == true
            }
            .toSet()
            .takeIf {
                it.isNotEmpty()
            }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Set<String>,
    ): NadelTransformFieldResult {
        val fieldsArgsToInterpret = state

        return NadelTransformFieldResult(
            newField = field.toBuilder()
                .normalizedArguments(
                    field.normalizedArguments
                        .asSequence()
                        .map { (key, value) ->
                            if (key in fieldsArgsToInterpret) {
                                key to NormalizedInputValue(
                                    value.typeName,
                                    StringValue(
                                        // Strips the ari:/… prefix
                                        (value.value as StringValue).value.substringAfterLast("/"),
                                    ),
                                )
                            } else {
                                key to value
                            }
                        }
                        .toMapStrictly()
                )
                .build(),
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: Set<String>,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }
}

@UseHook
class `ari-argument-transform` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>> = listOf(
        AriTestTransform(),
    )
}

@UseHook
class `ari-argument-transform-on-renamed-field` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>> = listOf(
        AriTestTransform(),
    )
}
