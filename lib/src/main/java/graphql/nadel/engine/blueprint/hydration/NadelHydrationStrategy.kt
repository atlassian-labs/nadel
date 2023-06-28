package graphql.nadel.engine.blueprint.hydration

sealed class NadelHydrationStrategy {
    object OneToOne : NadelHydrationStrategy()

    /**
     * Hydration where an array of values is fed into a scalar argument e.g.
     *
     * ```graphql
     * type Query {
     *   me: User
     *   user(id: ID!): User
     * }
     *
     * type User {
     *   friendIds: [ID!]!
     *   friends: [User] @hydrated(
     *     from: ["user"],
     *     arguments: [
     *       {name: "id", valueFromField: ["friendIds"]},
     *     ],
     *   )
     * }
     * ```
     *
     * That is, for N elements of `friendIds` we must make N calls to the `user` top level field.
     *
     * Another example:
     *
     * ```graphql
     * type Query {
     *   me: User
     *   user(id: ID!): User
     * }
     *
     * type User {
     *   cards: [BusinessCard]
     *   acquaintances: [User] @hydrated(
     *     from: ["user"],
     *     arguments: [
     *       {name: "id", valueFromField: ["cards", "userId"]},
     *     ],
     *   )
     * }
     *
     * type BusinessCard {
     *   userId: ID!
     * }
     * ```
     *
     * This is the same situation, for N cards we must make N calls to the `user` top level field.
     */
    class ManyToOne(val inputDefToSplit: NadelHydrationArgumentDef) : NadelHydrationStrategy()
}
