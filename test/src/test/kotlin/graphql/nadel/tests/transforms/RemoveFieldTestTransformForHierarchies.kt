package graphql.nadel.tests.transforms

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.enginekt.transform.result.json.JsonNodePathSegment
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError

data class State(val objectTypeNames: List<String>, val typeRejected: Boolean = false)

class RemoveFieldTestTransformForHierarchies : NadelTransform<State> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
    ): State? {
        val objectTypes = overallField.objectTypeNames.asSequence()
            .map {
                executionBlueprint.schema.getType(it) as GraphQLObjectType?
            }
            .filterNotNull()
            .toList()

        val objectTypesWhereForbidden = objectTypes
            .filter { objectType ->
                objectType.fieldDefinitions
                    .find { it.name == overallField.name }
                    ?.directives
                    ?.any { it.name == "toBeDeleted" }
                    ?: return@filter false
            }

        if (objectTypesWhereForbidden.isNotEmpty()) {
            return State(objectTypesWhereForbidden.map { it.name })
        }

        val filter = overallField.objectTypeNames.filter {
            val type = executionBlueprint.schema.getObjectType(it)
            if (type != null && type.directives.any { it.name.equals("toBeDeleted") }) {
                return@filter true
            }
            return@filter false
        }

        if (filter.isNotEmpty()) {
            return State(filter, true)
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
        if (state.objectTypeNames.isEmpty()) {
            return NadelTransformFieldResult(
                newField = null,
                artificialFields = listOf(
                    ExecutableNormalizedField.newNormalizedField()
                        .level(field.level)
                        .objectTypeNames(field.objectTypeNames.toList())
                        .fieldName(Introspection.TypeNameMetaFieldDef.name)
                        .parent(field.parent)
                        .alias("uuid_typename")
                        .build()
                )
            )
        }

        val originalTypeNames = ArrayList(field.objectTypeNames)
        field.objectTypeNames.removeAll(state.objectTypeNames.toSet())

        val artificialFields = if (!state.typeRejected || field == field.parent.children[0]) listOf(
            ExecutableNormalizedField.newNormalizedField()
                .level(field.level)
                .objectTypeNames(originalTypeNames)
                .fieldName(Introspection.TypeNameMetaFieldDef.name)
                .parent(field.parent)
                .alias("uuid_typename")
                .build()
        ) else emptyList()
        return NadelTransformFieldResult(
            newField = field,
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
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        if (state.objectTypeNames.isEmpty()) {
            return parentNodes.map { parentNode ->
                val destinationPath = parentNode.resultPath + overallField.resultKey

                NadelResultInstruction.Set(
                    subjectPath = destinationPath,
                    newValue = null,
                )
            } + NadelResultInstruction.AddError(
                ValidationError.newValidationError().description("aint gonna work").build()
            )
        }

        val map: MutableList<NadelResultInstruction> = mutableListOf()
        var errorAdded = false
        for (parentNode in parentNodes) {
            val nodeAt: JsonNode? = JsonNodeExtractor.getNodeAt(
                parentNode,
                JsonNodePath(listOf(JsonNodePathSegment.String("uuid_typename")))
            )
            if (!state.objectTypeNames.contains(nodeAt!!.value as String)) {
                continue
            }
            val destinationPath =
                if (state.typeRejected) parentNode.resultPath else parentNode.resultPath + overallField.resultKey

            map.add(
                NadelResultInstruction.Set(
                    subjectPath = destinationPath,
                    newValue = null,
                )
            )
            errorAdded = true
        }
        if (errorAdded) {
            if (!state.typeRejected || overallField == overallField.parent.children[0]) {
                map.add(
                    NadelResultInstruction.AddError(
                        ValidationError.newValidationError().description("aint gonna work").build()
                    )
                )
            }
        }
        return map
    }
}
