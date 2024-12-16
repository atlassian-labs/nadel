package graphql.nadel.engine.transform.query

import graphql.nadel.ServiceLike
import graphql.nadel.engine.util.toGraphQLErrorException
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.hooks.NadelDynamicServiceResolutionResult
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.schema.NadelDirectives.dynamicServiceDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLSchema

internal class DynamicServiceResolution(
    private val engineSchema: GraphQLSchema,
    private val executionHooks: NadelExecutionHooks,
    private val services: List<ServiceLike>,
) {

    /**
     * Checks if the field needs to have its service dynamically resolved
     */
    fun needsDynamicServiceResolution(
        topLevelField: ExecutableNormalizedField,
    ): Boolean =
        topLevelField.getFieldDefinitions(engineSchema)
            .asSequence()
            .filter {
                it.getAppliedDirective(dynamicServiceDirectiveDefinition.name) != null
            }
            .onEach {
                require(it.type.unwrapNonNull() is GraphQLInterfaceType) {
                    "Field annotated with ${dynamicServiceDirectiveDefinition.name} directive is expected to be of GraphQLInterfaceType"
                }
            }
            .filterNotNull()
            .any()

    /**
     * Resolves the service for a field
     */
    fun resolveServiceForField(field: ExecutableNormalizedField): ServiceLike {
        val result = executionHooks.resolveServiceForField(services, field)

        return when (result) {
            is NadelDynamicServiceResolutionResult.Error -> throw result.error.toGraphQLErrorException()
            is NadelDynamicServiceResolutionResult.Success -> result.service
        }
    }
}
