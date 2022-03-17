package graphql.nadel.engine.transform.hydration

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationObjectIdFieldBuilder.makeObjectIdFields
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.deepClone
import graphql.nadel.engine.util.resolveObjectTypes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

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

        val actorFieldOverallObjectTypeNames = getActorFieldOverallObjectTypenames(instruction)
        val fieldChildren = deepClone(fields = hydratedField.children)
            .mapNotNull { childField ->
                val objectTypesAreNotReturnedByActorField =
                    actorFieldOverallObjectTypeNames.none { it in childField.objectTypeNames }

                if (objectTypesAreNotReturnedByActorField) {
                    null
                } else {
                    childField.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(childField.objectTypeNames.filter { it in actorFieldOverallObjectTypeNames })
                        .build()
                }
            }
            .let { children ->
                children + makeObjectIdFields(aliasHelper, instruction)
            }

        return argBatches.map { argBatch ->
            makeActorQueries(
                instruction = instruction,
                fieldArguments = argBatch.mapKeys { (inputDef: NadelHydrationActorInputDef) -> inputDef.name },
                fieldChildren = fieldChildren,
            )
        }
    }

    private fun getActorFieldOverallObjectTypenames(
        instruction: NadelBatchHydrationFieldInstruction,
    ): Set<String> {
        val overallTypeName = instruction.actorFieldDef.type.unwrapAll().name

        val actorServiceSchema = instruction.actorService.schema
        val overallType = actorServiceSchema.getType(overallTypeName)
            ?: error("Unable to find overall type $overallTypeName")

        val actorFieldOverallObjectTypes = resolveObjectTypes(actorServiceSchema, overallType) { type ->
            error("Unable to resolve to object type: $type")
        }

        return actorFieldOverallObjectTypes
            .asSequence()
            .map { it.name }
            .toSet()
    }

    fun makeRequiredSourceFields(
        service: Service,
        aliasHelper: NadelAliasHelper,
        objectTypeName: GraphQLObjectTypeName,
        instructions: List<NadelGenericHydrationInstruction>,
    ): List<ExecutableNormalizedField> {
        val underlyingTypeName = service.blueprint.typeRenames.getUnderlyingName(overallTypeName = objectTypeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instructions
            .asSequence()
            .flatMap { it.sourceFields }
            .map {
                aliasHelper.toArtificial(
                    NFUtil.createField(
                        schema = service.underlyingSchema,
                        parentType = underlyingObjectType,
                        queryPathToField = it,
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
