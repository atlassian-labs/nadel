package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy.MatchObjectIdentifier
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInput
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationInputBuilder
import graphql.nadel.enginekt.transform.query.NFUtil
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.deepClone
import graphql.nadel.enginekt.util.unwrap
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal object NadelHydrationFieldsBuilder {
    fun makeActorQuery(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: AliasHelper,
        hydratedField: NormalizedField,
        parentNode: JsonNode,
    ): NormalizedField {
        return makeActorQuery(
            instruction = instruction,
            fieldArguments = NadelHydrationInputBuilder.getInputValues(
                instruction = instruction,
                aliasHelper = aliasHelper,
                hydratedField = hydratedField,
                parentNode = parentNode,
            ),
            fieldChildren = deepClone(fields = hydratedField.children),
        )
    }

    fun makeActorQueries(
        instruction: NadelBatchHydrationFieldInstruction,
        aliasHelper: AliasHelper,
        hydratedField: NormalizedField,
        parentNodes: List<JsonNode>,
    ): List<NormalizedField> {
        val argBatches = NadelBatchHydrationInputBuilder.getInputValueBatches(
            instruction = instruction,
            aliasHelper = aliasHelper,
            hydrationField = hydratedField,
            parentNodes = parentNodes,
        )

        val fieldChildren = deepClone(fields = hydratedField.children).let { children ->
            when (val objectIdField = makeObjectIdField(aliasHelper, instruction)) {
                null -> children
                else -> children + objectIdField
            }
        }

        return argBatches.map { argBatch ->
            makeActorQuery(
                instruction = instruction,
                fieldArguments = argBatch.mapKeys { (input: NadelHydrationActorInput) -> input.name },
                fieldChildren = fieldChildren,
            )
        }
    }

    fun makeFieldsUsedAsActorInputValues(
        service: Service,
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: AliasHelper,
        fieldCoordinates: FieldCoordinates,
        instruction: NadelGenericHydrationInstruction,
    ): List<NormalizedField> {
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(overallTypeName = fieldCoordinates.typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instruction.actorInputValues
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationActorInput.ValueSource.FieldResultValue>()
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

    private fun makeActorQuery(
        instruction: NadelGenericHydrationInstruction,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NormalizedField>,
    ): NormalizedField {
        return NFUtil.createField(
            schema = instruction.actorService.underlyingSchema,
            parentType = instruction.actorService.underlyingSchema.queryType,
            queryPathToField = instruction.queryPathToActorField,
            fieldArguments = fieldArguments,
            fieldChildren = fieldChildren,
        )
    }

    private fun makeObjectIdField(
        aliasHelper: AliasHelper,
        batchHydrationInstruction: NadelBatchHydrationFieldInstruction,
    ): NormalizedField? {
        return when (val matchStrategy = batchHydrationInstruction.batchHydrationMatchStrategy) {
            is MatchObjectIdentifier -> {
                val actorField = NadelHydrationUtil.getActorField(batchHydrationInstruction)
                val underlyingSchema = batchHydrationInstruction.actorService.underlyingSchema
                val objectTypes: List<GraphQLObjectType> = when (val type = actorField.type.unwrap(all = true)) {
                    is GraphQLObjectType -> listOf(type)
                    is GraphQLUnionType -> type.types.map { it as GraphQLObjectType }
                    is GraphQLInterfaceType -> underlyingSchema.getImplementations(type)
                    else -> error("When matching by object identifier, output type of actor field must be an object")
                }

                NormalizedField.newNormalizedField()
                    .objectTypeNames(objectTypes.map { it.name })
                    .fieldName(matchStrategy.objectId)
                    .alias(aliasHelper.getObjectIdentifierKey(matchStrategy.objectId))
                    .build()
            }
            else -> null
        }
    }
}
