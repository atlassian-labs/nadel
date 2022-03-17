package graphql.nadel.engine.transform.query

import graphql.GraphqlErrorException
import graphql.nadel.Service
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toGraphQLErrorException
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.schema.NadelDirectives.dynamicServiceDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLSchema

internal class DynamicServiceResolution(
    private val engineSchema: GraphQLSchema,
    private val serviceExecutionHooks: ServiceExecutionHooks,
    private val services: Map<String, Service>,
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
                    "field annotated with ${dynamicServiceDirectiveDefinition.name} directive is expected to be of GraphQLInterfaceType"
                }
            }
            .any { it != null }

    /**
     * Resolves the service for a field
     */
    fun resolveServiceForField(field: ExecutableNormalizedField): Service {
        val serviceOrError = serviceExecutionHooks.resolveServiceForField(services.values.toList(), field)
            ?: throw GraphqlErrorException.newErrorException()
                .message("Could not resolve service for field '${field.name}'")
                .path(field.queryPath.segments)
                .build()

        // Either or… but needs to be better
        val error = serviceOrError.error
        if (error != null) {
            throw error.toGraphQLErrorException()
        } else {
            return requireNotNull(serviceOrError.service)
        }
    }
}
