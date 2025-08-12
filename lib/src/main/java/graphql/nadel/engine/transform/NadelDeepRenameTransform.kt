package graphql.nadel.engine.transform

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.engine.transform.NadelDeepRenameTransform.TransformFieldContext
import graphql.nadel.engine.transform.NadelDeepRenameTransform.TransformOperationContext
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates

@Deprecated("Should be changed to a value class")
internal typealias GraphQLObjectTypeName = String

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
internal class NadelDeepRenameTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        /**
         * The instructions for the a [ExecutableNormalizedField].
         *
         * Note that we can have multiple transform instructions for one [ExecutableNormalizedField]
         * due to the multiple [ExecutableNormalizedField.objectTypeNames] e.g.
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
        val instructionsByObjectTypeNames: Map<String, NadelDeepRenameFieldInstruction>,
        /**
         * See [NadelAliasHelper]
         */
        val aliasHelper: NadelAliasHelper,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    /**
     * Determines whether a deep rename is applicable for the given [overallField].
     *
     * Creates a state with the deep rename instructions and the transform alias.
     */
    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        val deepRenameInstructions = transformContext.executionBlueprint
            .getTypeNameToInstructionMap<NadelDeepRenameFieldInstruction>(overallField)
        if (deepRenameInstructions.isEmpty()) {
            return null
        }

        return TransformFieldContext(
            transformContext,
            overallField,
            deepRenameInstructions,
            NadelAliasHelper.forField(tag = "deep_rename", overallField),
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
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        val objectTypesNoRenames =
            field.objectTypeNames.filterNot { it in transformContext.instructionsByObjectTypeNames }

        return NadelTransformFieldResult(
            newField = objectTypesNoRenames
                .takeIf(List<String>::isNotEmpty)
                ?.let {
                    field.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(it)
                        .build()
                },
            artificialFields = transformContext.instructionsByObjectTypeNames
                .map { (objectTypeWithRename, instruction) ->
                    makeDeepField(
                        transformContext,
                        transformer,
                        field,
                        objectTypeWithRename,
                        deepRename = instruction,
                    )
                }
                .let { deepFields ->
                    when (val typeNameField = makeTypeNameField(transformContext, field)) {
                        null -> deepFields
                        else -> deepFields + typeNameField
                    }
                },
        )
    }

    /**
     * Read [TransformFieldContext.instructionsByObjectTypeNames]
     *
     * In the case that there are multiple [FieldCoordinates] for a single [ExecutableNormalizedField]
     * we need to know which type we are dealing with, so we use this to add a `__typename`
     * selection to determine the behavior on [transformResult].
     *
     * This detail is omitted from most examples in this file for simplicity.
     */
    private fun makeTypeNameField(
        transformContext: TransformFieldContext,
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        val typeNamesWithInstructions = transformContext.instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return makeTypeNameField(
            aliasHelper = transformContext.aliasHelper,
            objectTypeNames = objectTypeNames,
            deferredExecutions = field.deferredExecutions,
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
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
        overallObjectTypeName: String,
        deepRename: NadelDeepRenameFieldInstruction,
    ): ExecutableNormalizedField {
        val service = transformContext.service
        val underlyingTypeName =
            transformContext.executionBlueprint.getUnderlyingTypeName(service, overallObjectTypeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return transformContext.aliasHelper.toArtificial(
            NFUtil.createField(
                schema = service.underlyingSchema,
                parentType = underlyingObjectType,
                queryPathToField = deepRename.queryPathToField,
                fieldArguments = field.normalizedArguments,
                fieldChildren = transformer.transform(field.children),
                deferredExecutions = field.deferredExecutions,
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
    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val overallField = transformContext.overallField

        val parentNodes = resultNodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.mapNotNull instruction@{ parentNode ->
            val instruction = transformContext.instructionsByObjectTypeNames.getInstructionForNode(
                executionBlueprint = transformContext.executionBlueprint,
                service = transformContext.service,
                aliasHelper = transformContext.aliasHelper,
                parentNode = parentNode,
            ) ?: return@instruction null

            val queryPathForSourceField = transformContext.aliasHelper.getQueryPath(instruction.queryPathToField)
            val sourceFieldNode = JsonNodeExtractor.getNodesAt(parentNode, queryPathForSourceField)
                .emptyOrSingle()

            when (sourceFieldNode) {
                null -> NadelResultInstruction.Set(
                    subject = parentNode,
                    key = NadelResultKey(overallField.resultKey),
                    newValue = null,
                )

                else -> NadelResultInstruction.Set(
                    subject = parentNode,
                    key = NadelResultKey(overallField.resultKey),
                    newValue = sourceFieldNode,
                )
            }
        }
    }
}

