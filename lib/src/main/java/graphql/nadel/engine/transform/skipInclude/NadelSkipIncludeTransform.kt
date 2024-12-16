package graphql.nadel.engine.transform.skipInclude

import graphql.execution.MergedField
import graphql.introspection.Introspection
import graphql.language.Field
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.ServiceLike
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.transform.skipInclude.NadelSkipIncludeTransform.State
import graphql.nadel.engine.util.resolveObjectTypes
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLSchema

/**
 * So the way `@skip` and `@include` work is that in the [graphql.normalized.ExecutableNormalizedOperationFactory]
 * is that they get automatically removed by the factory. Because of that, we never actually
 * get the fields. This causes bad outcomes for Nadel because we don't execute the query here.
 * We forward the query and we are _not_ allowed generate invalid queries with empty
 * selection sets.
 *
 * So here, we add back in a `__typename` field to ensure we don't have an empty selection.
 *
 * This should probably be a more generic "if no subselections add an empty one for removed fields".
 * But we'll deal with that separately.
 */
internal class NadelSkipIncludeTransform : NadelTransform<State> {
    companion object {
        private const val skipFieldName = "__skip"

        fun isSkipIncludeSpecialField(enf: ExecutableNormalizedField): Boolean {
            return enf.name == skipFieldName
        }
    }

    class State(
        val aliasHelper: NadelAliasHelper,
    )

    /**
     * So this transform is a bit odd. Normally transform operate on a specific field.
     *
     * However, in the case of a `@skip` the field with that directive is automatically removed.
     *
     * So in order to execute on the deleted field we insert a fake field back into the midst for
     * the transform API to pick up on.
     *
     * This should really not happen. The real fix is to execute on the parent of the deleted
     * field and to fix [getResultInstructions] to include `underlyingField` and not just `underlyingParentField`.
     */
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: ServiceLike,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        // This hacks together a child that will pass through here
        if (overallField.children.isEmpty()) {
            val mergedField = executionContext.query.getMergedField(overallField)
            if (hasAnyChildren(mergedField)) {
                // Adds a field so we can transform it
                overallField.children.add(createSkipField(executionBlueprint.engineSchema, overallField))
            }
        }

        return if (overallField.name == skipFieldName) {
            State(
                aliasHelper = NadelAliasHelper.forField(
                    tag = "skip_include",
                    field = overallField,
                ),
            )
        } else {
            null
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: ServiceLike,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            artificialFields = listOf(
                field.toBuilder()
                    .alias(state.aliasHelper.typeNameResultKey)
                    .fieldName(Introspection.TypeNameMetaFieldDef.name)
                    .build(),
            ),
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: ServiceLike,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }

    private fun hasAnyChildren(mergedField: MergedField?): Boolean {
        return mergedField?.fields?.any(::hasAnyChildren) == true
    }

    private fun hasAnyChildren(field: Field?): Boolean {
        return field?.selectionSet?.selections?.isNotEmpty() == true
    }

    private fun createSkipField(
        overallSchema: GraphQLSchema,
        parent: ExecutableNormalizedField,
    ): ExecutableNormalizedField {
        val objectTypeNames = parent.getFieldDefinitions(overallSchema)
            .asSequence()
            .map {
                it.type
            }
            .flatMap {
                // This resolves abstract types to object types
                resolveObjectTypes(overallSchema, it) { type ->
                    // Interface always resolves to object types
                    // Unions MUST contain object types https://spec.graphql.org/draft/#sec-Unions.Type-Validation
                    error("Unable to resolve to object type: $type")
                }
            }
            .map {
                it.name
            }
            .toList()

        return newNormalizedField()
            .objectTypeNames(objectTypeNames)
            .fieldName(skipFieldName)
            .build()
    }
}
