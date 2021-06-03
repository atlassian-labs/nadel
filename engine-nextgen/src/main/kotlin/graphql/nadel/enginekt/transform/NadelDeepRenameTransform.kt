package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.artificial.ArtificialFields
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor.getNodesAt
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

/**
 * A deep rename is a rename in where the field being "renamed" is not on the same level
 * as the deep rename declaration e.g.
 *
 * ```graphql
 * type Dog {
 *   name: String @renamed(from: ["details", "name"]) # This is the deep rename
 *   details: DogDetails # only in underlying schema
 * }
 *
 * type DogDetails {
 *   name: String
 * }
 * ```
 */
internal class NadelDeepRenameTransform : NadelTransform<NadelDeepRenameTransform.State> {
    data class State(
        /**
         * The instructions for the a [NormalizedField].
         *
         * Note that we can have multiple transform instructions for one [NormalizedField]
         * due to the multiple [NormalizedField.objectTypeNames] e.g.
         *
         * ```graphql
         * type Query {
         *   pets: [Pet]
         * }
         *
         * interface Pet {
         *   name: String
         * }
         *
         * type Dog implements Pet {
         *   name: String @renamed(from: ["collar", "name"])
         * }
         *
         * type Cat implements Pet {
         *   name: String @renamed(from: ["tag", "name"])
         * }
         * ```
         */
        val instructions: Map<FieldCoordinates, NadelDeepRenameFieldInstruction>,
        /**
         * See [ArtificialFields]
         */
        val artificialFields: ArtificialFields,
        /**
         * Stored for easy access in other functions.
         */
        val field: NormalizedField,
    )

    /**
     * Determines whether a deep rename is applicable for the given [field].
     *
     * Creates a state with the deep rename instructions and the transform alias.
     */
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        field: NormalizedField,
    ): State? {
        val deepRenameInstructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelDeepRenameFieldInstruction>(field)
        if (deepRenameInstructions.isEmpty()) {
            return null
        }

        return State(
            deepRenameInstructions,
            ArtificialFields("my_uuid"),
            field,
        )
    }

    /**
     * Changes the overall [field] to the fields from the underlying service
     * required to perform the deep rename.
     *
     * e.g. per the pet examples
     *
     * ```graphql
     * type Query {
     *   pets: [Pet]
     * }
     *
     * interface Pet {
     *   name: String
     * }
     *
     * type Dog implements Pet {
     *   name: String @renamed(from: ["collar", "name"])
     * }
     *
     * type Cat implements Pet {
     *   name: String @renamed(from: ["tag", "name"])
     * }
     * ```
     *
     * then given a query
     *
     * ```graphql
     * {
     *   pets {
     *     ... on Dog { name }
     *     ... on Cat { name }
     *   }
     * }
     * ```
     *
     * this function changes it to
     *
     * ```graphql
     * {
     *   pets {
     *     ... on Dog {
     *       collar { name }
     *     }
     *     ... on Cat {
     *       tag { name }
     *     }
     *   }
     * }
     * ```
     */
    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation, // this has an underlying schema
        service: Service,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            artificialFields = state.instructions.map { (coordinates, instruction) ->
                makeDeepField(
                    state,
                    transformer,
                    executionPlan,
                    service,
                    field,
                    coordinates,
                    deepRename = instruction,
                )
            } + makeTypeNameField(state),
        )
    }

    /**
     * Read [State.instructions]
     *
     * In the case that there are multiple [FieldCoordinates] for a single [NormalizedField]
     * we need to know which type we are dealing with, so we use this to add a `__typename`
     * selection to determine the behavior on [getResultInstructions].
     *
     * This detail is omitted from most examples in this file for simplicity.
     */
    private fun makeTypeNameField(
        state: State,
    ): NormalizedField {
        return NadelTransformUtil.makeTypeNameField(
            artificialFields = state.artificialFields,
            objectTypeNames = state.instructions.keys.map { it.typeName },
        )
    }

    /**
     * Read [transformField]
     *
     * This function actually creates the deep selection i.e. for
     *
     * ```graphql
     * name: String @renamed(from: ["collar", "name"])
     * ```
     *
     * this will actually create
     *
     * ```graphql
     * collar {
     *   name
     * }
     * ```
     */
    private suspend fun makeDeepField(
        state: State,
        transformer: NadelQueryTransformer.Continuation,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField,
        fieldCoordinates: FieldCoordinates,
        deepRename: NadelDeepRenameFieldInstruction,
    ): NormalizedField {
        val underlyingTypeName = fieldCoordinates.typeName.let { overallTypeName ->
            executionPlan.typeRenames[overallTypeName]?.underlyingName ?: overallTypeName
        }

        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return state.artificialFields.toArtificial(
            NadelPathToField.createField(
                schema = service.underlyingSchema,
                parentType = underlyingObjectType,
                pathToField = deepRename.pathToSourceField,
                fieldArguments = emptyMap(),
                fieldChildren = transformer.transform(field.children),
            ),
        )
    }

    /**
     * This function moves the referenced field to the deep rename field.
     *
     * i.e. for
     *
     * ```graphql
     * type Dog {
     *   name: String @renamed(from: ["collar", "name"])
     * }
     * ```
     *
     * then for an object in the GraphQL response
     *
     * ```graphql
     * {
     *   "__typename": "Dog",
     *   "collar": { "name": "Luna" }
     * }
     * ```
     *
     * it will return the instructions
     *
     * ```
     * Copy(subjectPath=/collar/name, destinationPath=/)
     * Remove(subjectPath=/collar)
     * ```
     */
    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField, // Overall field
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = getNodesAt(
            result.data,
            field.listOfResultKeys.dropLast(1),
            flatten = true,
        )

        return parentNodes.flatMap { parentNode ->
            val deepRenameInstruction = getMatchingDeepRenameInstruction(parentNode, state, executionPlan)
                ?: return@flatMap emptyList()

            val nodeToMove = getNodesAt(parentNode, getPathOfNodeToMove(state, deepRenameInstruction))
                .emptyOrSingle() ?: return@flatMap emptyList()

            listOf(
                NadelResultInstruction.Copy(
                    subjectPath = nodeToMove.path,
                    destinationPath = parentNode.path + field.resultKey,
                ),
            )
        }
    }

    /**
     * Read [State.alias]
     *
     * For a schema
     *
     * ```graphql
     * type Dog {
     *   name: String @renamed(from: ["collar", "name"])
     * }
     * ```
     *
     * @return if the value of [State.alias] is `hello_world` then it returns `[hello_world__collar, name]`
     */
    private fun getPathOfNodeToMove(state: State, instruction: NadelDeepRenameFieldInstruction): List<String> {
        return state.artificialFields.mapPathToResultKeys(instruction.pathToSourceField)
    }

    /**
     * Note: this can be null if the type condition was not met
     */
    private fun getMatchingDeepRenameInstruction(
        parentNode: JsonNode,
        state: State,
        executionPlan: NadelExecutionPlan,
    ): NadelDeepRenameFieldInstruction? {
        val overallTypeName = NadelTransformUtil.getOverallTypename(
            executionPlan = executionPlan,
            artificialFields = state.artificialFields,
            node = parentNode,
        )
        return state.instructions[makeFieldCoordinates(overallTypeName, state.field.name)]
    }
}

