package graphql.nadel.enginekt.transform.deepRename

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getForField
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.nadel.enginekt.transform.query.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

inline fun <K, reified T> Map<K, *>.filterValuesOfType(): Map<K, T> {
    @Suppress("UNCHECKED_CAST")
    return filterValues {
        it is T
    } as Map<K, T>
}

internal class NadelDeepRenameTransform : NadelTransform<NadelDeepRenameTransform.State> {
    data class State(
        val instructions: Map<FieldCoordinates, NadelDeepRenameFieldInstruction>,
        val alias: String,
    )

    override fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
    ): State? {
        val instructions = executionBlueprint.fieldInstructions.getForField(field)
        if (instructions.isEmpty()) {
            return null
        }

        val deepRenameInstructions = instructions
            .filterValuesOfType<FieldCoordinates, NadelDeepRenameFieldInstruction>()
        if (deepRenameInstructions.isEmpty()) {
            return null
        }

        return State(
            deepRenameInstructions,
            "my_uuid"
            // UUID.randomUUID().toString(),
        )
    }

    override fun transformField(
        transformer: NadelQueryTransformer.Continuation,
        service: Service, // this has an underlying schema
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        // TODO: add typename field here
        return NadelTransformFieldResult(
            newField = null,
            extraFields = state.instructions.map { (coordinates, instruction) ->
                createDeepField(
                    transformer,
                    executionBlueprint,
                    service,
                    field,
                    coordinates,
                    deepRename = instruction,
                ).transform {
                    it.alias(getFirstFieldResultKey(state, instruction))
                }
            },
        )
    }

    private fun createDeepField(
        transformer: NadelQueryTransformer.Continuation,
        blueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
        fieldCoordinates: FieldCoordinates,
        deepRename: NadelDeepRenameFieldInstruction,
    ): NormalizedField {
        val underlyingTypeName = fieldCoordinates.typeName.let { overallTypeName ->
            blueprint.typeInstructions[overallTypeName]?.underlyingName ?: overallTypeName
        }

        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return NadelPathToField.createField(
            schema = service.underlyingSchema,
            parentType = underlyingObjectType,
            pathToField = deepRename.pathToSourceField,
            fieldChildren = transformer.transform(field.children),
        )
    }

    override fun getResultInstructions(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField, // Overall field
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            result.data,
            field.listOfResultKeys.dropLast(1),
            flatten = true,
        )

        return parentNodes.flatMap { parentNode ->
            @Suppress("UNCHECKED_CAST") // Ensure the result is a Map, return if null
            val parentMap = parentNode.value as JsonMap? ?: return@flatMap emptyList()
            val instruction = getFieldInstructionFor(parentMap, state)

            val nodeToMove = JsonNodeExtractor.getNodesAt(parentNode, getPathToSourceField(state, instruction))
                .emptyOrSingle() ?: return@flatMap emptyList()

            listOf(
                NadelResultInstruction.Copy(
                    subjectPath = nodeToMove.path,
                    destinationPath = parentNode.path + field.resultKey,
                ),
                NadelResultInstruction.Remove(
                    subjectPath = parentNode.path + getFirstFieldResultKey(state, instruction),
                ),
                NadelResultInstruction.Remove(
                    subjectPath = parentNode.path + getTypeNameResultKey(state),
                ),
            )
        }
    }

    private fun getPathToSourceField(state: State, instruction: NadelDeepRenameFieldInstruction): List<String> {
        val firstKey = getFirstFieldResultKey(state, instruction)
        return listOf(firstKey) + instruction.pathToSourceField.drop(1)
    }

    private fun getFirstFieldResultKey(state: State, instruction: NadelDeepRenameFieldInstruction): String {
        return state.alias + "__" + instruction.pathToSourceField.first()
    }

    private fun getTypeNameResultKey(state: State): String {
        return state.alias + Introspection.typeNameField
    }

    private fun getFieldInstructionFor(
        parentMap: JsonMap,
        state: State,
    ): NadelDeepRenameFieldInstruction {
        // TODO: handle type renames
        // Note, we add a typename in transformField, so it should NEVER be null
        val typeName = parentMap[getTypeNameResultKey(state)]
            ?: error("Typename must never be null")

        val fieldName = state.instructions.keys.first().fieldName
        return state.instructions[makeFieldCoordinates(typeName as String, fieldName)]
            ?: error("No instruction for '$typeName'")
    }
}

object Introspection {
    const val typeNameField = "__typename"
}
