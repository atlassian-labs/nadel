package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.query.NFUtil
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.FieldCoordinates

internal object NadelHydrationFieldsBuilder {
    fun getActorQuery(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: AliasHelper,
        hydrationField: NormalizedField,
        parentNode: JsonNode,
    ): NormalizedField {
        return getActorQuery(
            instruction,
            hydrationField,
            fieldArguments = NadelHydrationInputBuilder.getInputValues(
                instruction,
                aliasHelper,
                hydrationField,
                parentNode,
            ),
        )
    }

    fun getActorQuery(
        instruction: NadelGenericHydrationInstruction,
        hydrationField: NormalizedField,
        fieldArguments: Map<String, NormalizedInputValue>,
    ): NormalizedField {
        return NFUtil.createField(
            schema = instruction.actorService.underlyingSchema,
            parentType = instruction.actorService.underlyingSchema.queryType,
            queryPathToField = instruction.actorFieldQueryPath,
            fieldArguments = fieldArguments,
            fieldChildren = hydrationField.children,
        )
    }

    fun getArtificialFields(
        service: Service,
        executionPlan: NadelExecutionPlan,
        aliasHelper: AliasHelper,
        fieldCoordinates: FieldCoordinates,
        instruction: NadelGenericHydrationInstruction,
    ): List<NormalizedField> {
        val underlyingTypeName = executionPlan.getUnderlyingTypeName(overallTypeName = fieldCoordinates.typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instruction.actorInputValues
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationArgumentValueSource.FieldResultValue>()
            .map { valueSource ->
                aliasHelper.toArtificial(
                    NFUtil.createField(
                        schema = service.underlyingSchema,
                        parentType = underlyingObjectType,
                        queryPathToField = valueSource.queryPathToField,
                        fieldArguments = emptyMap(),
                        fieldChildren = emptyList(), // This must be a leaf node
                    ),
                )
            }
            .toList()
    }
}
