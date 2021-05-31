package graphql.nadel.enginekt.transform.hydration

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.language.AstPrinter
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform.State
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.nadel.enginekt.transform.query.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.mapToArrayList
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField
import graphql.normalized.NormalizedQueryToAstCompiler
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

internal class NadelHydrationTransform(
    private val engine: NextgenEngine,
) : NadelTransform<State> {
    data class State(
        val instructions: Map<FieldCoordinates, NadelHydrationFieldInstruction>,
        /**
         * The field in question for the transform, stored for quick access when
         * the [State] is passed around.
         */
        val field: NormalizedField,
        val alias: String,
    )

    override suspend fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        field: NormalizedField,
    ): State? {
        val hydrationInstructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelHydrationFieldInstruction>(field)

        return if (hydrationInstructions.isEmpty()) {
            null
        } else {
            State(
                hydrationInstructions,
                field,
                alias = "hydration_uuid",
            )
        }
    }

    override suspend fun transformField(
        transformer: NadelQueryTransformer.Continuation,
        service: Service,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            extraFields = state.instructions.flatMap { (fieldCoordinates, instruction) ->
                NadelHydrationFieldsBuilder.getExtraFields(service, executionPlan, fieldCoordinates, instruction)
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
        // TODO: DRY this code, this is copied from deep rename
        return newNormalizedField()
            .alias(getTypeNameResultKey(state))
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(state.instructions.keys.mapToArrayList { it.typeName })
            .build()
    }

    override suspend fun getResultInstructions(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryResultKeyPath = field.listOfResultKeys.dropLast(1),
            flatten = true,
        )

        return parentNodes.flatMap {
            hydrate(
                parentNode = it,
                state = state,
                executionPlan = executionPlan,
                hydrationField = field,
            )
        }
    }

    private fun hydrate(
        parentNode: JsonNode,
        state: State,
        executionPlan: NadelExecutionPlan,
        hydrationField: NormalizedField, // Field asking for hydration from the overall query
    ): List<NadelResultInstruction> {
        @Suppress("UNCHECKED_CAST")
        val instruction = getMatchingInstruction(
            parentNode.value as JsonMap,
            state,
            executionPlan,
        ) ?: return emptyList()

        val sourceField = NadelPathToField.createField(
            schema = instruction.sourceService.underlyingSchema,
            parentType = instruction.sourceService.underlyingSchema.queryType,
            pathToField = instruction.pathToSourceField,
            fieldArguments = NadelHydrationArgumentsBuilder.createSourceFieldArgs(
                instruction,
                parentNode,
                hydrationField,
            ),
            fieldChildren = hydrationField.children,
        )

        // TODO: execute it
        println(
            AstPrinter.printAst(
                NormalizedQueryToAstCompiler.compileToDocument(
                    listOf(sourceField),
                ),
            ),
        )

        return emptyList()
    }

    /**
     * Read [alias]
     *
     * @return the aliased value of the GraphQL introspection field `__typename`
     */
    private fun getTypeNameResultKey(state: State): String {
        return state.alias + TypeNameMetaFieldDef.name
    }

    /**
     * Note: this can be null if the type condition was not met
     */
    private fun getMatchingInstruction(
        parentMap: JsonMap,
        state: State,
        executionPlan: NadelExecutionPlan,
    ): NadelHydrationFieldInstruction? {
        val underlyingTypeName = parentMap[getTypeNameResultKey(state)] as? String
            ?: error("${TypeNameMetaFieldDef.name} must never be null")

        val overallTypeName = executionPlan.getOverallTypeName(underlyingTypeName)

        return state.instructions[makeFieldCoordinates(overallTypeName, state.field.name)]
    }
}
