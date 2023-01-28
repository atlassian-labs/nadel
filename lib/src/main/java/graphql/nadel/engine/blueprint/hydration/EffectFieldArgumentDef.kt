package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition

data class EffectFieldArgumentDef(
    val name: String,
    val effectArgumentDef: GraphQLArgument,
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
        data class FromResultValue(
            val queryPathToField: NadelQueryPath,
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
        data class FromArgumentValue(
            val argumentName: String,
            val argumentDefinition: GraphQLArgument,
            val defaultValue: NormalizedInputValue?,
        ) : ValueSource()
    }
}

