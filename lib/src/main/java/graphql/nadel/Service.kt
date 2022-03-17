package graphql.nadel

import graphql.nadel.engine.blueprint.NadelServiceBlueprint
import graphql.schema.GraphQLSchema

class Service(
    val name: String,
    /**
     * These are the types as they are defined in the underlying service's schema.
     *
     * There are no renames, hydrations etc.
     *
     * You should avoid using this unless you are doing validations etc.
     */
    val underlyingSchema: GraphQLSchema,
    /**
     * Callback provided to Nadel that performs the GraphQL call to this service.
     */
    val serviceExecution: ServiceExecution,
    /**
     * These are the GraphQL definitions that a service contributes to the OVERALL schema.
     */
    val definitionRegistry: NadelDefinitionRegistry,
) {
    /**
     * For now, this is the actual overall schema. But in the future this will be a subset
     * of the overall schema that this [Service] contributed.
     */
    lateinit var schema: GraphQLSchema
        internal set

    /**
     * The blueprint associated with this [Service].
     */
    lateinit var blueprint: NadelServiceBlueprint
        internal set

    override fun toString(): String {
        return "Service(name='$name')"
    }
}
