package graphql.nadel.enginekt.transform.query

import graphql.Assert
import graphql.GraphqlErrorException
import graphql.nadel.Service
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.toGraphQLErrorException
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.schema.NadelDirectives
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil

internal class DynamicServiceResolution(
    private val overallSchema: GraphQLSchema,
    private val serviceExecutionHooks: ServiceExecutionHooks,
    private val services: Collection<Service>
) {

    /**
     * Checks if the field needs to have its service dynamically resolved
     */
    fun needsDynamicServiceResolution(
        topLevelField: ExecutableNormalizedField
    ): Boolean =
        topLevelField.getFieldDefinitions(overallSchema)
            .asSequence()
            .filter {
                it.getDirective(NadelDirectives.DYNAMIC_SERVICE_DIRECTIVE_DEFINITION.name) != null
            }
            .onEach {
                Assert.assertTrue(GraphQLTypeUtil.unwrapNonNull(it.type) is GraphQLInterfaceType) {
                    String.format(
                        "field annotated with %s directive is expected to be of GraphQLInterfaceType",
                        NadelDirectives.DYNAMIC_SERVICE_DIRECTIVE_DEFINITION.name
                    )
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

        if (serviceOrError.error != null) {
            throw serviceOrError.error.toGraphQLErrorException()
        } else {
            return serviceOrError.service
        }
    }
}