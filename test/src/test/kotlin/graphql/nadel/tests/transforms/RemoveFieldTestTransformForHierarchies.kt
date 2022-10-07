package graphql.nadel.tests.transforms

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodePath
import graphql.nadel.engine.transform.result.json.JsonNodePathSegment
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError

open class State(val targetTypeNames: Set<String>)
class FieldRemoved(targetTypeNames: Set<String>) : State(targetTypeNames)
class TypeRemoved(targetTypeNames: Set<String>) : State(targetTypeNames)

private const val directiveName = "toBeDeleted"
private const val artificialFieldAlias = "uuid_typename"


class RemoveFieldTestTransformForHierarchies : NadelTransform<State> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {

        val fieldGraphQLObjectTypes = overallField.objectTypeNames
            .asSequence()
            .map {
                executionBlueprint.engineSchema.getType(it) as GraphQLObjectType?
            }
            .filterNotNull()
            .toSet()

        val objectTypesWhereThisFieldIsRemoved = fieldGraphQLObjectTypes.asSequence()
            .filter { objectType ->
                objectType.getFieldDefinition(overallField.name)
                    ?.hasAppliedDirective(directiveName) == true
            }
            .map { it.name }
            .toSet()

        if (objectTypesWhereThisFieldIsRemoved.isNotEmpty()) {
            return FieldRemoved(objectTypesWhereThisFieldIsRemoved)
        }

        val removedTypes = fieldGraphQLObjectTypes.asSequence()
            .filter { it.hasAppliedDirective(directiveName) }
            .map { it.name }
            .toSet()

        if (removedTypes.isNotEmpty()) {
            return TypeRemoved(removedTypes)
        }

        return null
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        val originalTypeNames = ArrayList(field.objectTypeNames)
        field.objectTypeNames.removeAll(state.targetTypeNames.toSet())
        val artificialFields =
            if (state is FieldRemoved || field == field.parent.children[0]) {
                listOf(
                    ExecutableNormalizedField.newNormalizedField()
                        .level(field.level)
                        .objectTypeNames(originalTypeNames)
                        .fieldName(Introspection.TypeNameMetaFieldDef.name)
                        .parent(field.parent)
                        .alias(artificialFieldAlias)
                        .build()
                )
            } else emptyList()
        return NadelTransformFieldResult(
            newField = if (field.objectTypeNames.isEmpty()) null else field,
            artificialFields = artificialFields
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
        val instructions: MutableList<NadelResultInstruction> = mutableListOf()
        var errorAdded = false

        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )
        for (parentNode in parentNodes) {
            val artificialTypeNameJsonNode = JsonNodeExtractor.getNodeAt(
                parentNode,
                JsonNodePath(listOf(JsonNodePathSegment.String(artificialFieldAlias)))
            )
            val typenameIfFieldWasRemoved = artificialTypeNameJsonNode?.value
            if (!state.targetTypeNames.contains(typenameIfFieldWasRemoved)) {
                continue
            }
            val destinationPath =
                when (state) {
                    is TypeRemoved -> parentNode.resultPath
                    else /* is FieldRemoved */ -> parentNode.resultPath + overallField.resultKey
                }

            instructions.add(
                NadelResultInstruction.Set(
                    subjectPath = destinationPath,
                    newValue = null,
                )
            )
            errorAdded = true
        }
        if (errorAdded) {
            if (state is FieldRemoved || overallField == overallField.parent.children[0]) {
                instructions.add(
                    NadelResultInstruction.AddError(
                        ValidationError.newValidationError().description("aint gonna work").build()
                    )
                )
            }
        }
        return instructions
    }
}
