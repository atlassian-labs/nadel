package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.transform.GraphQLObjectTypeName
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationInputBuilder
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationObjectIdFieldBuilder.makeObjectIdField
import graphql.nadel.enginekt.transform.query.NFUtil
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.deepClone
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType

internal object NadelHydrationFieldsBuilder {
    fun makeActorQueries(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        fieldToHydrate: ExecutableNormalizedField,
        parentNode: JsonNode,
    ): List<ExecutableNormalizedField> {
        return NadelHydrationInputBuilder.getInputValues(
            instruction = instruction,
            aliasHelper = aliasHelper,
            fieldToHydrate = fieldToHydrate,
            parentNode = parentNode,
        ).map { args ->
            makeActorQueries(
                instruction = instruction,
                fieldArguments = args,
                fieldChildren = deepClone(fields = fieldToHydrate.children),
            )
        }
    }

    fun makeBatchActorQueries(
        executionBlueprint: NadelOverallExecutionBlueprint,
        instruction: NadelBatchHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        hydratedField: ExecutableNormalizedField,
        parentNodes: List<JsonNode>,
        hooks: ServiceExecutionHooks,
    ): List<ExecutableNormalizedField> {
        val argBatches = NadelBatchHydrationInputBuilder.getInputValueBatches(
            instruction = instruction,
            aliasHelper = aliasHelper,
            hydrationField = hydratedField,
            parentNodes = parentNodes,
            hooks = hooks
        )

        val actorFieldOverallObjectTypeNames = getActorFieldOverallObjectTypenames(instruction, executionBlueprint)
        val fieldChildren = deepClone(fields = hydratedField.children)
            .mapNotNull { childField ->
                val objectTypesAreNotReturnedByActorField =
                    actorFieldOverallObjectTypeNames.intersect(childField.objectTypeNames).isEmpty()
                if (objectTypesAreNotReturnedByActorField) {
                    return@mapNotNull null
                }
                filterObjectTypeNamesToMatchActorField(childField, actorFieldOverallObjectTypeNames)
                return@mapNotNull childField
            }
            .let { children ->
                when (val objectIdField = makeObjectIdField(executionBlueprint, aliasHelper, instruction)) {
                    null -> children
                    else -> children + objectIdField
                }
            }

        return argBatches.map { argBatch ->
            makeActorQueries(
                instruction = instruction,
                fieldArguments = argBatch.mapKeys { (inputDef: NadelHydrationActorInputDef) -> inputDef.name },
                fieldChildren = fieldChildren,
            )
        }
    }

    private fun filterObjectTypeNamesToMatchActorField(
        field: ExecutableNormalizedField,
        actorFieldOverallObjectTypeNames: Set<String>
    ) {
        field.objectTypeNames.removeIf { !actorFieldOverallObjectTypeNames.contains(it) }
    }

    private fun getActorFieldOverallObjectTypenames(
        instruction: NadelBatchHydrationFieldInstruction,
        executionBlueprint: NadelOverallExecutionBlueprint
    ): Set<String> {
        val actorFieldUnderlyingType = instruction.actorFieldDef.type.unwrapAll()
        val overallTypeName =
            executionBlueprint.getOverallTypeName(instruction.actorService, actorFieldUnderlyingType.name)
        val actorFieldOverallObjectTypes: List<GraphQLType> =
            when (val actorFieldOverallType = executionBlueprint.schema.getType(overallTypeName)!!) {
                is GraphQLInterfaceType -> executionBlueprint.schema.getImplementations(actorFieldOverallType)
                is GraphQLUnionType -> actorFieldOverallType.types
                else -> {
                    listOf(actorFieldOverallType)
                }
            }
        return actorFieldOverallObjectTypes.map { (it as GraphQLNamedType).name }
            .toSet()
    }

    fun makeFieldsUsedAsActorInputValues(
        service: Service,
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        objectTypeName: GraphQLObjectTypeName,
        instructions: List<NadelGenericHydrationInstruction>,
    ): List<ExecutableNormalizedField> {
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(service, overallTypeName = objectTypeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instructions.asSequence()
            .flatMap { it.actorInputValueDefs }
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationActorInputDef.ValueSource.FieldResultValue>()
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

    private fun makeActorQueries(
        instruction: NadelGenericHydrationInstruction,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
    ): ExecutableNormalizedField {
        return NFUtil.createField(
            schema = instruction.actorService.underlyingSchema,
            parentType = instruction.actorService.underlyingSchema.queryType,
            queryPathToField = instruction.queryPathToActorField,
            fieldArguments = fieldArguments,
            fieldChildren = fieldChildren,
        )
    }
}
