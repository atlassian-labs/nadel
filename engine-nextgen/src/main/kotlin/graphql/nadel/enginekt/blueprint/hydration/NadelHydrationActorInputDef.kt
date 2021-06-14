package graphql.nadel.enginekt.blueprint.hydration

import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition

data class NadelHydrationActorInputDef(
    val name: String,
    val actorArgumentDef: GraphQLArgument,
    val valueSource: ValueSource,
) {
    sealed class ValueSource {
        /**
         * Uses a value from a field in the same object (or its children) as input e.g.
         *
         * ```graphql
         * type Issue {
         *   id: ID! # Value used as argument to issueId
         *   owner: User @hydrated(from: ["issueOwner"], args: [
         *      {name: "issueId" valueFromField: ["id"]}
         *   ])
         * }
         * ```
         */
        data class FieldResultValue(
            val queryPathToField: QueryPath,
            val fieldDefinition: GraphQLFieldDefinition,
        ) : ValueSource()

        /**
         * Uses a value from a field in the same object (or its children) as input e.g.
         *
         * ```graphql
         * type Issue {
         *   secret(password: String): JSON @hydrated(from: ["issueSecret"], args: [
         *     {name: "password", valueFromArg: "password"}
         *   ])
         * }
         * ```
         */
        data class ArgumentValue(
            val argumentName: String,
            val argumentDefinition: GraphQLArgument,
        ) : ValueSource()
    }
}

