package graphql.nadel.enginekt.transform.skipInclude

import graphql.execution.MergedField
import graphql.introspection.Introspection
import graphql.language.Field
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.transform.skipInclude.SkipIncludeTransform.State
import graphql.nadel.enginekt.util.resolveObjectTypes
import graphql.nadel.enginekt.util.toBuilder
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
internal class SkipIncludeTransform : NadelTransform<State> {
    companion object {
        private const val skipFieldName = "__skip"
    }

    class State(
        val aliasHelper: NadelAliasHelper,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        // This hacks together a child that will pass through here
        if (overallField.children.isEmpty()) {
            val mergedField = executionContext.query.getMergedField(overallField)
            if (hasAnyChildren(mergedField)) {
                // Adds a field so we can transform it
                overallField.children.add(createSkipField(executionBlueprint.schema, overallField))
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
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
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
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
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
