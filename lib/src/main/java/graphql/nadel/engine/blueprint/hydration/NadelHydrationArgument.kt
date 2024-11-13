package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition

sealed class NadelHydrationArgument {
    abstract val backingArgumentDef: GraphQLArgument

    val name: String
        get() = backingArgumentDef.name

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
    data class SourceField(
        override val backingArgumentDef: GraphQLArgument,
        val pathToSourceField: NadelQueryPath,
        val sourceFieldDef: GraphQLFieldDefinition,
    ) : NadelHydrationArgument()

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
    data class VirtualFieldArgument(
        override val backingArgumentDef: GraphQLArgument,
        val virtualFieldArgumentDef: GraphQLArgument,
        val defaultValue: NormalizedInputValue?,
    ) : NadelHydrationArgument() {
        val virtualFieldArgumentName: String get() = virtualFieldArgumentDef.name
    }

    /**
     * Represents a static argument value, which is hardcoded in the source code. e.g.
     *
     * ```graphql
     * type Issue {
     *   id: ID!
     *   owner: User @hydrated(from: ["issueOwner"], args: [
     *      {name: "issueId" value: "issue123"}
     *   ])
     * }
     * ```
     */
    data class StaticValue(
        override val backingArgumentDef: GraphQLArgument,
        val normalizedInputValue: NormalizedInputValue,
    ) : NadelHydrationArgument()

    /**
     * Collates the unused arguments on the virtual field and puts them as key value pairs
     * into a JSON object that gets passed into an argument on the backing field that is
     * annotated with `@hydrationRemainingArguments` e.g.
     *
     * ```graphql
     * type Issue {
     *   id: ID!
     *   owner(unusedArg: String, somethingElse: Boolean, admin: Boolean): User
     *     @hydrated(
     *       from: "issueOwner",
     *       arguments: [
     *         {name: "admin" value: "$argument.admin"}
     *       ]
     *     )
     * }
     * type Query {
     *   issueOwner(
     *
     *   ): User
     * }
     * ```
     *
     * Would produce a JSON object like
     *
     */
    data class RemainingVirtualFieldArguments(
        override val backingArgumentDef: GraphQLArgument,
        val remainingArgumentNames: List<String>,
    ) : NadelHydrationArgument()
}

