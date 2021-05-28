package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.nadel.enginekt.transform.query.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor.getNodesAt
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.filterValuesOfType
import graphql.nadel.enginekt.util.mapToArrayList
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField
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
         * When we query the underlying service we ensure to add an alias to the fields
         * we insert in [NadelDeepRenameTransform.transformField] to ensure no clashes
         * in the query namespace.
         *
         * The examples omit this detail for simplicity, but this manifests as such, given:
         *
         * ```graphql
         * type Cat implements Pet {
         *   name: String @renamed(from: ["tag", "name"])
         * }
         * ```
         *
         * and a query:
         *
         * ```
         * {
         *   pet {
         *     ... on Cat { name }
         *   }
         * }
         * ```
         *
         * then the query to the underlying service should look something similar to:
         *
         * ```
         * {
         *   pet {
         *     ... on Cat {
         *       my_alias__tag: tag { name }
         *     }
         *   }
         * }
         * ```
         */
        val alias: String,
    )

    /**
     * Determines whether a deep rename is applicable for the given [field].
     *
     * Creates a state with the deep rename instructions and the transform alias.
     */
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
    override fun transformField(
        transformer: NadelQueryTransformer.Continuation,
        service: Service, // this has an underlying schema
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            extraFields = state.instructions.map { (coordinates, instruction) ->
                makeDeepField(
                    transformer,
                    executionPlan,
                    service,
                    field,
                    coordinates,
                    deepRename = instruction,
                ).transform {
                    it.alias(getFirstFieldResultKey(state, instruction))
                }
            } + makeTypeNameField(state, state.instructions),
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
        instructions: Map<FieldCoordinates, NadelDeepRenameFieldInstruction>,
    ): NormalizedField {
        return newNormalizedField()
                .alias(getTypeNameResultKey(state))
                .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(instructions.keys.mapToArrayList { it.typeName })
            .build()
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
    private fun makeDeepField(
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

        return NadelPathToField.createField(
            schema = service.underlyingSchema,
            parentType = underlyingObjectType,
            pathToField = deepRename.pathToSourceField,
            fieldChildren = transformer.transform(field.children),
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
    override fun getResultInstructions(
        userContext: Any?,
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
            @Suppress("UNCHECKED_CAST") // Ensure the result is a Map, return if null
            val parentMap = parentNode.value as JsonMap? ?: return@flatMap emptyList()
            val deepRenameInstruction = getMatchingDeepRename(parentMap, state, executionPlan)

            val nodeToMove = getNodesAt(parentNode, getPathOfNodeToMove(state, deepRenameInstruction))
                    .emptyOrSingle() ?: return@flatMap emptyList()

            listOf(
                NadelResultInstruction.Copy(
                    subjectPath = nodeToMove.path,
                    destinationPath = parentNode.path + field.resultKey,
                ),
                NadelResultInstruction.Remove(
                        subjectPath = parentNode.path + getFirstFieldResultKey(state, deepRenameInstruction),
                ),
                NadelResultInstruction.Remove(
                    subjectPath = parentNode.path + getTypeNameResultKey(state),
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
        val firstKey = getFirstFieldResultKey(state, instruction)
        return listOf(firstKey) + instruction.pathToSourceField.drop(1)
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
     * @return if the value of [State.alias] is `hello_world` then it returns `hello_world__collar`
     */
    private fun getFirstFieldResultKey(state: State, instruction: NadelDeepRenameFieldInstruction): String {
        return state.alias + "__" + instruction.pathToSourceField.first()
    }

    /**
     * Read [State.alias]
     *
     * @return the aliased value of the GraphQL introspection field `__typename`
     */
    private fun getTypeNameResultKey(state: State): String {
        return state.alias + TypeNameMetaFieldDef.name
    }

    private fun getMatchingDeepRename(
        parentMap: JsonMap,
        state: State,
        executionPlan: NadelExecutionPlan
    ): NadelDeepRenameFieldInstruction {
        val underlyingTypeName = parentMap[getTypeNameResultKey(state)] as? String
            ?: error("Typename must never be null")
        val overallTypeName = executionPlan.getOverallTypeName(underlyingTypeName)

        val fieldName = state.instructions.keys.first().fieldName
        return state.instructions[makeFieldCoordinates(overallTypeName as String, fieldName)]
            ?: error("No instruction for '$overallTypeName'")
    }
}

