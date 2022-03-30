package graphql.nadel.tests.hooks

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
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.hooks.RedactDataTransform.State
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

object Permissions {
    fun isAllowedToAccess(cloudId: String): Boolean {
        return cloudId == "fwang.atlassian.net"
    }
}

class RedactDataTransform : NadelTransform<State> {
    data class State(
        val overallField: ExecutableNormalizedField,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        val shouldTransformRun = overallField
            .objectTypeNames
            .map { objectTypeName ->
                executionBlueprint.engineSchema.getTypeAs<GraphQLObjectType>(objectTypeName)
            }
            .any { objectType ->
                objectType.hasAppliedDirective("redactable")
            }

        return if (shouldTransformRun) {
            State(
                overallField,
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
        val objectTypesToCloudIdFields: Map<GraphQLObjectType, GraphQLFieldDefinition> = field.objectTypeNames
            .map { objectTypeName ->
                executionBlueprint.engineSchema.getTypeAs<GraphQLObjectType>(objectTypeName)
            }
            .associateWith { objectType -> // objectType = Page
                objectType.fields  // [id, cloudID]
                    .first {
                        it.hasAppliedDirective("CloudId")
                    }
            }

        return NadelTransformFieldResult(
            field,
            artificialFields = objectTypesToCloudIdFields.map { (objectType, fieldDef) ->
                newNormalizedField()
                    .objectTypeNames(listOf(objectType.name))
                    .fieldName(fieldDef.name)
                    .build()
            },
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
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
        )

        return parentNodes
            .filter { parentNode ->
                // parentNode = JsonNode(
                //   resultPath=[myActivities, page],
                //   value={id=page-1, cloudId=andi-site.atlassian.net}
                // )
                @Suppress("UNCHECKED_CAST")
                val parentNodeAsMap = parentNode.value as JsonMap

                // println(parentNodes)
                // {id=page-1, cloudId=andi-site.atlassian.net}

                val cloudId = parentNodeAsMap["cloudId"] as String

                Permissions.isAllowedToAccess(cloudId) == false
            }
            .map { parentNode ->
                NadelResultInstruction.Set(
                    subjectPath = parentNode.resultPath,
                    newValue = null,
                )
            }
    }
}

@UseHook
class `can-redact-data` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            RedactDataTransform()
        )
}

@UseHook
class `does-not-redact-data` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            RedactDataTransform()
        )
}
