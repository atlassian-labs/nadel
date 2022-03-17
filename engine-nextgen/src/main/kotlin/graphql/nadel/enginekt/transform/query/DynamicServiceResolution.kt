package graphql.nadel.enginekt.transform.query

import graphql.GraphqlErrorException
import graphql.nadel.Service
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.toGraphQLErrorException
import graphql.nadel.enginekt.util.unwrapNonNull
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.schema.NadelDirectives.dynamicServiceDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLSchema

internal class DynamicServiceResolution(
    private val engineSchema: GraphQLSchema,
    private val serviceExecutionHooks: ServiceExecutionHooks,
    private val services: List<Service>,
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
        val serviceOrError = serviceExecutionHooks.resolveServiceForField(services, field)
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
