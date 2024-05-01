package graphql.nadel

import graphql.schema.GraphQLSchema
import graphql.schema.idl.TypeDefinitionRegistry

open class Service(
    val name: String,
    /**
     * These are the types as they are defined in the underlying service's schema.
     *
     *
     * There are no renames, hydrations etc.
     */
    val underlyingSchema: GraphQLSchema,
    // this is not enough in the future as we need to allow for dynamic delegationExecution
    val serviceExecution: ServiceExecution,
    /**
     * These are the GraphQL definitions that a service contributes to the OVERALL schema.
     */
    val definitionRegistry: NadelDefinitionRegistry,
) {
    override fun toString(): String {
        return "Service{name='$name'}"
    }
}
